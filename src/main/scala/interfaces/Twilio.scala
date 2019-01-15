package interfaces

import simulacrum._
import scalaz._
import Scalaz._

import models._

trait Twilio[F[_]] {
  def sendSMS(
    twilioNumber: TwilioNumber,
    msg: TwilioSMS
  ): F[Unit]

  def toTwilioTextMessage(msg: ValidatedTextMessage): F[TwilioTextMessage]
  def validateNumber(num: PhoneNumber): F[ValidatedPhoneNumber]
}
