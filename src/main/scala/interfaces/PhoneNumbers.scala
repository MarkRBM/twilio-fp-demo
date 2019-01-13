package interfaces

import java.util.UUID
import scala.concurrent.duration.{ Duration, FiniteDuration }
import simulacrum._
import scalaz._
import Scalaz._

import utilities._
import models._

trait PhoneNumbersRepository[F[_]] {
  def getAll: F[IList[TwilioNumber]]
  def getAllAvailable: F[IList[TwilioNumber]]
  def getAvailable: F[Option[TwilioNumber]]
  def save(ph: TwilioNumber): F[TwilioNumber]
  def saveAll(phs: NonEmptyList[TwilioNumber]): F[Int]
  def delete(ph: TwilioNumber): F[Unit]
  def find(uuid: UUID): F[Option[TwilioNumber]]
  def findByNumberSid(sid: String): F[Option[TwilioNumber]]
  def findByTwilioNumber(tn: String): F[Option[TwilioNumber]]
  def setUnavailableFor(
    ph: TwilioNumber,
    until: Epoch
  ): F[Unit]
  def setAvailable(ph: TwilioNumber): F[Unit]
  def scheduleSetAvailable(ph: TwilioNumber, d: Duration): F[Unit]
}
