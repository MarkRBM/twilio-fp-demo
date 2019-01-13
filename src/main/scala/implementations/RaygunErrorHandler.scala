package implementations

import com.mindscapehq.raygun4java.core.{ RaygunClient, RaygunClientFactory }
import scalaz.zio._
import scalaz.zio.interop._

import interfaces.ErrorHandler
import models._
import utilities.ServerConfig

final class RaygunErrorHandler(config: ServerConfig) extends ErrorHandler {
  private val raygunClientFactory: Option[RaygunClientFactory] =
    config.raygunApiKey
      .map(k => new RaygunClientFactory(k).withTag("TwilioDemoApp"))

  private val raygunClient = () => raygunClientFactory.map(_.newClient())

  def recovering: PartialFunction[Throwable, Unit] = _ match {
    case e: Throwable => {
      println(s"we erroring in here ${e.getMessage}")
      e.printStackTrace
      raygunClient().map(_.send(e))
      ()
    }
  }
}
