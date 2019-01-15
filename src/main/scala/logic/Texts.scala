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
  conf: ServerConfig
)(implicit F: Monad[F], L: Logger[F]) {
  def sendText(t: TextMessage): F[Unit] =
    for {
      twilioNumber <- phoneNumbers.getTwilioNumber
      _ = println("something")
      _            <- Logger[F].info(s"Got twilio number")
      validTo      <- texts.validateNumber(t.to)
      _            <- Logger[F].info(s"to is valid")
      msg <- texts.toTwilioTextMessage(
              toValidatedTxt(t, None, validTo)
            )
      sent <- msg match {
               case txt: TwilioSMS => texts.sendSMS(twilioNumber, txt)
             }
      _ <- Logger[F].info(s"Message sent")
    } yield sent


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
