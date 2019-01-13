package utilities

import com.ovoenergy.fs2.kafka.ConsumerSettings
import implementations.doobs.TransactorProvider
import org.apache.kafka.clients.consumer._
import models.ProxyInfo
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._
import fs2._

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
  implicit val ProxyInfoMeta: Meta[ProxyInfo] = Meta[String]
    .xmap(((info: String) => ProxyInfo(info)), (info: ProxyInfo) => info.id)
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

  def scheduleExecution[R](q: ConnectionIO[R], d: Duration)(
    implicit transactorProvider: TransactorProvider[Task]
  ): Task[Unit] =
    for {
      _ <- (for {
            _   <- IO.sleep(d)
            res <- execute[R](q)
          } yield res).fork
    } yield ()
}

case class KafkaConfig(
  topicName: String,
  pollTimeout: Duration,
  maxParallelism: Int,
  brokers: String,
  groupId: String
) {
  def getConsumerSettings: ConsumerSettings = ConsumerSettings(
    pollTimeout = pollTimeout,
    maxParallelism = maxParallelism,
    nativeSettings = Map(
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "false",
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG  -> brokers,
      ConsumerConfig.GROUP_ID_CONFIG           -> groupId,
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG  -> "latest"
    )
  )
}
final case class ServerConfig(
  callbackurl: String,
  proxyDuration: Duration,
  twilioRootAccId: String,
  twilioRootAccToken: String,
  kafkaConfig: KafkaConfig,
  raygunApiKey: Option[String]
)

object FS2Utils {
  def liftPipe[F[_], A, B](f: A => F[B]): Pipe[F, A, B] = _.evalMap(f)
  def liftSink[F[_], A](f: A => F[Unit]): Sink[F, A]    = liftPipe[F, A, Unit](f)
}
