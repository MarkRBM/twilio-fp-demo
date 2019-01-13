package implementations.twilio

import com.twilio.exception.ApiException
import com.twilio.http.HttpMethod
import io.chrisdavenport.log4cats.Logger
import java.net.URI
import java.time.Instant
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.collection.JavaConverters._

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
import com.twilio.rest.proxy.v1.service.PhoneNumber
import com.twilio.rest.proxy.v1.Service
import com.twilio.rest.proxy.v1.service.Session
import com.twilio.rest.proxy.v1.service.session.Participant
import com.twilio.rest.proxy.v1.service.session.participant.MessageInteraction

import interfaces._
import models._
import utilities._

object Proxies {
  def createService(implicit L: Logger[Task]): Task[String] =
    Logger[Task].info("About to create service") *> IO.point {
      val s = Service
        .creator(s"demoProxyService")
        .create()
      s.getSid()
    }
  def getService(implicit L: Logger[Task]): Task[Option[String]] =
    for {
      _ <- Logger[Task].info("about to get service")
      res <- IO.point {
              val services     = Service.reader.read().asScala
              var srv: Service = null
              for (s <- services) {
                println(
                  s"Service: ${s.getUniqueName()}, sid: ${s.getSid()}"
                )
                if (srv == null) srv = s
              }
              Option(srv).map(
                a => a.getSid()
              )
            }
      _ <- Logger[Task].info(s"Got the service $res")
    } yield res

  def createSession(
    serviceId: String,
    from: String,
    to: String,
    twilioNumber: String
  )(implicit L: Logger[Task]): Task[String] =
    Logger[Task].info("About to create session") *> IO.point {
      val session = Session
        .creator(serviceId)
        .setUniqueName(
          s"demoProxyService-$from-$to-$twilioNumber-${Epoch.now}"
        )
        .create()
      session.getSid()
    }

  def getSession(
    serviceId: String,
    from: String,
    to: String,
    twilioNumber: String
  )(implicit L: Logger[Task]): Task[Option[String]] =
    Logger[Task].info("About to try and get session") *> IO.point {
      val sessions = Session
        .reader(serviceId)
        .setUniqueName(s"demoProxyService-$from-$to-$twilioNumber")
        .read()
        .asScala
      var sesh: Session = null
      for (s <- sessions) {
        println(
          s"Session: ${s.getUniqueName()}, sid: ${s.getSid()}"
        )
        if (sesh == null) sesh = s
      }
      Option(sesh).map(
        a => a.getSid()
      )
    }

  def createParticipant(
    serviceId: String,
    sessionId: String,
    participantNumber: String,
    participantName: String
  )(implicit L: Logger[Task]): Task[String] =
    Logger[Task].info(s"About to add participant $participantNumber") *> IO.point {
      println(
        s"adding $participantNumber as the $participantName as a participant"
      )
      val p = Participant
        .creator(serviceId, sessionId, participantNumber)
        .setFriendlyName(participantName)
        .create()
      p.getSid()
    }
  def addTwilioPhoneNumber(
    serviceId: String,
    twilioNumber: String
  )(implicit L: Logger[Task]): Task[Unit] =
    (for {
      _ <- Logger[Task]
            .info(s"About to add twilioNumber $twilioNumber")
            .liftM[OptionT]
      tNum <- OptionT(
               MessagingPhoneNumbers.getSidByNumber(twilioNumber)
             )
      _ <- Task.point {
            Try(PhoneNumber.creator(serviceId).setSid(tNum).create()) match {
              case Success(res) => res
              case Failure(ex)  => Task.fail(ex)
            }
          }.liftM[OptionT]

    } yield ()).run.map(_.getOrElse(()))

  def deleteTwilioPhoneNumber(
    serviceId: String,
    twilioNumber: String
  )(implicit L: Logger[Task]): Task[Unit] =
    (for {
      _ <- Logger[Task]
            .info(s"About to delete twilioNumber $twilioNumber from service")
            .liftM[OptionT]
      tNum <- OptionT(
               MessagingPhoneNumbers.getSidByNumber(twilioNumber)
             )
      _ <- Task.point {
            Try(PhoneNumber.deleter(serviceId, tNum).delete()) match {
              case Success(res) => res
              case Failure(ex)  => Task.fail(ex)
            }
          }.liftM[OptionT]

    } yield ()).run.map(_.getOrElse(()))

  def sendInitialMessage(
    serviceId: String,
    sessionId: String,
    phoneId: String,
    textBody: String
  )(implicit L: Logger[Task]): Task[Unit] =
    Logger[Task].info("About to send initial message") *> IO.point {
      MessageInteraction
        .creator(
          serviceId,
          sessionId,
          phoneId,
          s"$textBody"
        )
        .create()
      ()
    }
  def endSession(
    serviceId: String,
    sessionId: String
  )(implicit L: Logger[Task]): Task[Unit] =
    Logger[Task].info("About to delete session $sessionId") *> IO.point {
      Session.deleter(serviceId, sessionId).delete()
      ()
    }
}
