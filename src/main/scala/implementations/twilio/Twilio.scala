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

final class TwilioImpl(implicit L: Logger[Task])
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

  def toTwilioTextMessage(msg: ValidatedTextMessage): Task[TwilioTextMessage] =
    IO.point(TwilioSMS(msg.from, msg.to, msg.body))

  def validateNumber(num: PhoneNumber): Task[ValidatedPhoneNumber] =
    for {
      validNumber  <- NumberLookup.look(num.num)
      toBeReturned = ValidatedPhoneNumber(validNumber.getPhoneNumber.toString)
    } yield toBeReturned

}
object TwilioImpl {
  var needToInit = true
  def apply(conf: ServerConfig)(implicit l: Logger[Task]): TwilioImpl = {
    if (needToInit) {
      TwilioSDK.init(conf.twilioRootAccId, conf.twilioRootAccToken)
      needToInit = false
    }
    new TwilioImpl
  }
}
