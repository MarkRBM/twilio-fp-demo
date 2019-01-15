package utilities

import implementations.doobs.TransactorProvider
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

import scalaz.zio.IO
import scalaz.zio.interop._
import scalaz.zio.interop.Task
import scalaz.zio.interop.scalaz72._
import scalaz.zio.interop.catz._

import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.Types
import scalaz.zio.interop.Task

final case class Epoch(millis: Long) extends AnyVal {
  def +(d: FiniteDuration): Epoch    = Epoch(millis + d.toMillis)
  def diff(e: Epoch): FiniteDuration = (e.millis - millis).millis
}

object Epoch {
  def now: Epoch = Epoch(Instant.now().toEpochMilli)
}

object DoobieUtils {
  implicit val UUIDMeta: Meta[UUID] = Meta[String]
    .xmap(((id: String) => UUID.fromString(id)), (uuid: UUID) => uuid.toString)
  implicit val EpochMeta: Meta[Epoch] = Meta[Timestamp].xmap(
    ((date: Timestamp) => Epoch(date.toInstant.toEpochMilli())),
    ((epoch: Epoch) => new Timestamp(epoch.millis))
  )

  def execute[R](q: ConnectionIO[R])(
    implicit
    transactorProvider: TransactorProvider[Task]
  ): Task[R] =
    for {
      tx  <- transactorProvider.getTransactor
      res <- q.transact(tx)
    } yield res
}

final case class ServerConfig(
  twilioRootAccId: String,
  twilioRootAccToken: String,
  raygunApiKey: Option[String]
)
