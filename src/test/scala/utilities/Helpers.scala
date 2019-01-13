package tests.utilities

import scala.concurrent.duration._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import scalaz._
import Scalaz._
import scalaz.zio.interop.Task
import shims.effect._
import utilities.{ KafkaConfig, ServerConfig }
import models._

object Helpers {
  type F[a] = State[World, a]
  implicit def logger[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    NoOpLogger.impl[F]

  final case class World(
    pendingProxies: IList[Proxy],
    twilioPhoneNumbers: IList[TwilioNumber]
  )
  val mockConf: ServerConfig = ServerConfig(
    "",
    1 minutes,
    "",
    "",
    KafkaConfig(
      "test-topic-name",
      1 seconds,
      1,
      "test-brokers",
      "test-group-id"
    ),
    Some("testRaygunApiKey")
  )

  // val AI: AppInfo[Task] = new AppInfo[Task] {
  //   def getDBInfo(c: Customer): Task[DatabaseInformation] =
  //     Task.point(
  //       DatabaseInformation(
  //         c.appCode,
  //         "Test User",
  //         "testDBPW",
  //         "testAddress",
  //         "testDBName"
  //       )
  //     )
  // }
}
