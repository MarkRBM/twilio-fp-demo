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

object StatefulPhoneNumbers {
  import Helpers._
  import State.{ get, modify }
  import StateT._

  def apply: PhoneNumbersRepository[F] = new PhoneNumbersRepository[F] {
    def getAvailable: F[Option[TwilioNumber]] =
      get.map(w => w.twilioPhoneNumbers.find(_.inUse == false))
  }

}
