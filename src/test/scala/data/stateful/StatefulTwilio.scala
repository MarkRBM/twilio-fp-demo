package tests.data.statefulimplementations

import interfaces._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import logic.{ PhoneNumbers, Texts }
import simulacrum._
import scalaz._
import Scalaz._
import scalaz.zio.interop.Task
import shims.effect._
import cats.effect.Sync
import cats.implicits._
import utilities._
import models._
import tests.utilities._

object StatefulTwilio {
  import Helpers._
  import State.{ get, modify }
  import StateT._

  def apply: Twilio[F] = new Twilio[F] {
    def sendSMS(
      twilioNumber: TwilioNumber,
      msg: TwilioSMS
    ): F[Unit] = get.map(_ => ())
    def toTwilioTextMessage(msg: ValidatedTextMessage): F[TwilioTextMessage] =
      get.map(_ => TwilioSMS(msg.from, msg.to, msg.body))

    def validateNumber(num: PhoneNumber): F[ValidatedPhoneNumber] =
      get.map(_ => ValidatedPhoneNumber(num.num))

  }

}
