package logic

import io.chrisdavenport.log4cats.Logger
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scalaz.zio.IO
import simulacrum._
import scalaz._
import Scalaz._

import utilities._
import models._
import interfaces._

final class Texts[F[_]](
  texts: Twilio[F],
  phoneNumbers: PhoneNumbers[F],
  proxies: Proxies[F],
  conf: ServerConfig
)(implicit F: Monad[F], L: Logger[F]) {
  def sendText(t: TextMessage): F[Unit] =
    for {
      twilioNumber <- phoneNumbers.getTwilioNumber
      _ = println("something")
      _            <- Logger[F].info(s"Got twilio number")
      validTo      <- texts.validateNumber(t.to)
      _            <- Logger[F].info(s"to is valid")
      validFrom    <- setUpProxy(t.from, validTo, twilioNumber)
      msg <- texts.toTwilioTextMessage(
              toValidatedTxt(t, validFrom, validTo)
            )
      sent <- msg match {
               case txt: TwilioSMS => texts.sendSMS(twilioNumber, txt)
               case txt: TwilioMMS => texts.sendMMS(twilioNumber, txt)
             }
      _ <- Logger[F].info(s"Message sent")
    } yield sent

  private def setUpProxy(
    from: Option[PhoneNumber],
    validTo: ValidatedPhoneNumber,
    twilioNumber: TwilioNumber
  ): F[Option[ValidatedPhoneNumber]] =
    (for {
      f         <- OptionT(F.pure(from))
      _         <- Logger[F].info(s"Allowing proxy").liftM[OptionT]
      validFrom <- texts.validateNumber(f).liftM[OptionT]
      _ <- phoneNumbers
            .setInUse(
              twilioNumber,
              Epoch.now + FiniteDuration(
                conf.proxyDuration.toMinutes,
                TimeUnit.MINUTES
              )
            )
            .liftM[OptionT]
      _ <- proxies
            .createProxy(validFrom, validTo, twilioNumber)
            .liftM[OptionT]
      _ <- Logger[F]
            .info(
              s"Scheduling the release of the number if no proxy started"
            )
            .liftM[OptionT]
      _ <- phoneNumbers
            .scheduleSetAvailable(twilioNumber, conf.proxyDuration)
            .liftM[OptionT]
      _ <- Logger[F]
            .info(
              s"Scheduled the release of the number if no proxy started"
            )
            .liftM[OptionT]
    } yield validFrom).run

  private def toValidatedTxt(
    txt: TextMessage,
    from: Option[ValidatedPhoneNumber],
    to: ValidatedPhoneNumber
  ): ValidatedTextMessage = txt match {
    case txt: SMS =>
      ValidatedSMS(
        from,
        to,
        if (from.isDefined)
          s"${txt.body} Reply to this message to chat with them"
        else txt.body
      )
    // case txt: MMS => ValidatedMMS(from, to)
    case txt: ValidatedTextMessage => txt
  }
}
