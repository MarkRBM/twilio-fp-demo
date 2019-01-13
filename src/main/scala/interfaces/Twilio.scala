package interfaces

import java.util.UUID
import scala.concurrent.duration.Duration
import simulacrum._
import scalaz._
import Scalaz._

import models._

trait Twilio[F[_]] {
  def sendSMS(
    twilioNumber: TwilioNumber,
    msg: TwilioSMS
  ): F[Unit]

  def sendMMS(
    twilioNumber: TwilioNumber,
    msg: TwilioMMS
  ): F[Unit]

  def toTwilioTextMessage(msg: ValidatedTextMessage): F[TwilioTextMessage]
  def validateNumber(num: PhoneNumber): F[ValidatedPhoneNumber]
  def getNewNumber: F[TwilioNumber]

  def startProxy(
    in: Proxy,
    initialTextBody: String
  ): F[Option[ProxyInfo]]
  def endProxy(in: Proxy): F[Unit]
  def scheduleProxyEnd(
    in: Proxy,
    d: Duration,
    afterEnding: (Proxy) => F[Unit]
  ): F[Unit]
}
