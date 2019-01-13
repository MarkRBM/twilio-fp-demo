package implementations

import com.twilio.exception.ApiException
import com.twilio.http.HttpMethod
import io.chrisdavenport.log4cats.Logger
import java.net.URI
import java.time.Instant
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import simulacrum._
import scalaz._
import Scalaz._
import scalaz.zio._
import scalaz.zio.interop._
import scalaz.zio.interop.scalaz72._

import com.twilio.{ Twilio => TwilioSDK }
import com.twilio.http.TwilioRestClient
import com.twilio.`type`.{ PhoneNumber => TwilioSDKPhoneNumber }
import com.twilio.rest.api.v2010.account.Message
import com.twilio.converter.Promoter

import interfaces._
import models._
import utilities._
import implementations.twilio._

final class TwilioImpl(conf: ServerConfig)(implicit L: Logger[Task])
    extends Twilio[Task] {

  def sendSMS(
    twilioNumber: TwilioNumber,
    msg: TwilioSMS
  ): Task[Unit] =
    for {
      _ <- Logger[Task].info(
            s" Sending SMS for TwilioNumber: $twilioNumber, From: ${twilioNumber.num} To: ${msg.to.num} Body: ${msg.body}"
          )
      m = Message
        .creator(
          new TwilioSDKPhoneNumber(msg.to.num),
          new TwilioSDKPhoneNumber(twilioNumber.num),
          msg.body
        )
        .create()
      _ <- Logger[Task].info(s"SENDING MESSAGE WITH SID: ${m.getSid()}")
    } yield ()

  def sendMMS(
    twilioNumber: TwilioNumber,
    msg: TwilioMMS
  ): Task[Unit] =
    for {
      _ <- Logger[Task].info(
            s"Sending MMS TwilioNumber: $twilioNumber, From: ${twilioNumber.num} To: ${msg.to.num} Body: ${msg.body}"
          )
      m = Message
        .creator(
          new TwilioSDKPhoneNumber(msg.to.num),
          new TwilioSDKPhoneNumber(twilioNumber.num),
          msg.body
        )
        .setMediaUrl(Promoter.listOfOne(URI.create(msg.mediaUrl)))
        .create()
      _ <- Logger[Task].info(s"SENDING MESSAGE WITH SID: ${m.getSid()}")
    } yield ()

  def toTwilioTextMessage(msg: ValidatedTextMessage): Task[TwilioTextMessage] =
    IO.point(TwilioSMS(msg.from, msg.to, msg.body))

  def validateNumber(num: PhoneNumber): Task[ValidatedPhoneNumber] =
    for {
      validNumber  <- NumberLookup.look(num.num)
      toBeReturned = ValidatedPhoneNumber(validNumber.getPhoneNumber.toString)
    } yield toBeReturned

  def getNewNumber: Task[TwilioNumber] =
    for {
      _         <- Logger[Task].info(s"Getting new number")
      newNumber <- MessagingPhoneNumbers.create(conf)
      _         <- Logger[Task].info(s"Got new number ${newNumber.num} for")
    } yield newNumber

  def startProxy(
    in: Proxy,
    initialTextBody: String
  ): Task[Option[ProxyInfo]] =
    Logger[Task].info(s"We want to start a proxy for $in") *>
      (for {
        serviceId <- OptionT(Proxies.getService)
                      .orElse(Proxies.createService.liftM[OptionT])
        _ <- Logger[Task]
              .info(s"StartProxy: Got serviceId $serviceId")
              .liftM[OptionT]
        sessionId <- Proxies
                      .createSession(
                        serviceId,
                        in.from,
                        in.to,
                        in.twilioNumber
                      )
                      .liftM[OptionT]
        _ <- Logger[Task]
              .info(s"StartProxy: Got sessionId $sessionId")
              .liftM[OptionT]
        _ <- Proxies
              .addTwilioPhoneNumber(serviceId, in.twilioNumber)
              .catchSome {
                case _: ApiException => Task.point(())
              }
              .liftM[OptionT]
        hostParticipantId <- Proxies
                              .createParticipant(
                                serviceId,
                                sessionId,
                                in.to,
                                "host"
                              )
                              .liftM[OptionT]
        visitorParticipantId <- Proxies
                                 .createParticipant(
                                   serviceId,
                                   sessionId,
                                   in.from,
                                   "visitor"
                                 )
                                 .liftM[OptionT]
        _ <- Proxies
              .sendInitialMessage(
                serviceId,
                sessionId,
                visitorParticipantId,
                initialTextBody
              )
              .liftM[OptionT]
      } yield ProxyInfo(sessionId)).run

  def endProxy(in: Proxy): Task[Unit] =
    (for {
      _ <- Logger[Task]
            .info("Ending Proxy: About to start really stopping the proxy.")
            .liftM[OptionT]
      _         <- Logger[Task].info("Ending Proxy").liftM[OptionT]
      serviceId <- OptionT(Proxies.getService)
      _ <- Logger[Task]
            .info(s"Ending Proxy: Got service $serviceId")
            .liftM[OptionT]
      sessionToBeKilled <- OptionT(Task.point(in.info.map(_.id)))
                            .orElse(
                              OptionT(
                                Proxies.getSession(
                                  serviceId,
                                  in.from,
                                  in.to,
                                  in.twilioNumber
                                )
                              )
                            )
      _ <- Logger[Task]
            .info(s"Ending Proxy: Got session $sessionToBeKilled")
            .liftM[OptionT]
      _ <- Proxies
            .endSession(serviceId, sessionToBeKilled)
            .liftM[OptionT]
      _ <- Proxies
            .deleteTwilioPhoneNumber(serviceId, in.twilioNumber)
            .liftM[OptionT]
      _ <- MessagingPhoneNumbers
            .resetNumberSettingsAfterProxyEnds(in.twilioNumber, conf)
            .liftM[OptionT]
    } yield ()).run.map(_.getOrElse(()))

  def scheduleProxyEnd(
    in: Proxy,
    d: Duration,
    afterEnding: (Proxy) => Task[Unit]
  ): Task[Unit] =
    for {
      _ <- Logger[Task].info(
            s"Scheduling the ending of proxy ${in.twilioNumber} ${d} in the future"
          )
      _ <- (for {
            _ <- IO.sleep(conf.proxyDuration)
            _ <- endProxy(in)
            _ <- afterEnding(in)
          } yield ()).fork
      _ <- Logger[Task].info(
            s"Scheduled the ending of proxy ${in.twilioNumber} ${d} in the future"
          )
    } yield ()

  private def toCustomerCallbackUrl(appCode: String, url: String) =
    s"$url/text-replies"
}
object TwilioImpl {
  var needToInit = true
  def apply(conf: ServerConfig)(implicit l: Logger[Task]): TwilioImpl = {
    if (needToInit) {
      TwilioSDK.init(conf.twilioRootAccId, conf.twilioRootAccToken)
      needToInit = false
    }
    new TwilioImpl(conf)
  }
}
