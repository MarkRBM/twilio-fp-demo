package interfaces

import java.util.UUID
import scala.concurrent.duration.Duration
import simulacrum._
import scalaz._
import Scalaz._

import models._

trait ProxiesRepository[F[_]] {
  def find(to: String, twilioNumber: String): F[Option[Proxy]]
  def find(uuid: UUID): F[Option[Proxy]]
  def findByTwilioNumber(tn: String): F[Option[Proxy]]
  def save(p: Proxy): F[UUID]
  def update(p: Proxy): F[Proxy]
  def delete(p: Proxy): F[Unit]
  def scheduleDelete(p: Proxy, d: Duration): F[Unit]
}
