package http

import cats.{ MonadError }
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.{ Decoder, HCursor }
import io.circe.syntax._

import cats.implicits._

import org.http4s._, org.http4s.dsl.Http4sDsl

import org.http4s.circe._

import org.http4s.circe.CirceEntityEncoder._

import org.http4s.dsl.io._
import org.http4s.scalaxml._

import cats.effect.Effect

import interfaces._
import models._
import logic.Texts
import utilities.Epoch

class TextHttpService[F[_]] extends Http4sDsl[F] {
  import ParamMatchers._

  implicit val decodeTextMessageRequest: Decoder[TextMessageRequest] =
    new Decoder[TextMessageRequest] {
      final def apply(c: HCursor): Decoder.Result[TextMessageRequest] =
        for {
          to   <- c.downField("to").as[String]
          body <- c.downField("body").as[String]
          txt = SMS(
            None,
            UnvalidatedPhoneNumber(to),
            body
          )
        } yield TextMessageRequest(txt)

    }

  def getService(
    texts: Texts[F],
    errorHandler: ErrorHandler
  )(
    implicit F: Effect[F],
    L: Logger[F],
    ME: MonadError[F, Throwable]
  ): HttpService[F] = HttpService[F] {
    case GET -> Root / "health" =>
      for {
        _   <- Logger[F].debug("Starting HealthCheck")
        res <- Ok("healthy".asJson)
        _   <- Logger[F].debug("HealthCheck finished")
      } yield res

    case req @ POST -> Root / "send-text" =>
      for {
        _      <- Logger[F].debug("Got HTTP request to send a text")
        txtreq <- req.decodeJson[TextMessageRequest]
        _ <- texts
              .sendText(txtreq.textMessage)
              .recover(errorHandler.recovering)
        resp <- Ok("sent".asJson)
        _    <- Logger[F].debug("Successfully handled HTTP request to send text")
      } yield resp
  }
}

object ParamMatchers {
  object ToMatcher   extends QueryParamDecoderMatcher[String]("To")
  object BodyMatcher extends QueryParamDecoderMatcher[String]("Body")
}
