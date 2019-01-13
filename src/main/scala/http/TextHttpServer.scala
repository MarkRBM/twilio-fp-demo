package http

import io.chrisdavenport.log4cats.Logger
import scalaz.zio.interop._
import scalaz.zio.interop.catz._

import fs2.Stream
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

import cats.effect.Effect

import interfaces.ErrorHandler
import logic._

object TextHttpServer {

  def httpStream[F[_]: Effect: Logger](
    texts: Texts[F],
    proxy: Proxies[F],
    eh: ErrorHandler
  )(implicit ex: ExecutionContext): Stream[F, ExitCode] =
    BlazeBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .mountService(
        new TextHttpService[F].getService(texts, proxy, eh),
        "/"
      )
      .serve
}
