package interfaces

import simulacrum._
import scalaz._
import Scalaz._

import utilities._
import models._

trait PhoneNumbersRepository[F[_]] {
  def getAvailable: F[Option[TwilioNumber]]
}
