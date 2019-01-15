package tests.utilities

import scala.concurrent.duration._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import scalaz._
import Scalaz._
import scalaz.zio.interop.Task
import shims.effect._
import utilities.ServerConfig
import models._

object Helpers {
  type F[a] = State[World, a]
  implicit def logger[F[_]: Applicative]: SelfAwareStructuredLogger[F] =
    NoOpLogger.impl[F]

  final case class World(
    twilioPhoneNumbers: IList[TwilioNumber]
  )
  val mockConf: ServerConfig = ServerConfig(
    "",
    "",
    Some("testRaygunApiKey")
  )
}
