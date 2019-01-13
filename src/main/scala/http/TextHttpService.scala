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
          from <- c.downField("proxy").as[Option[String]]
          to   <- c.downField("to").as[String]
          body <- c.downField("body").as[String]
          txt = SMS(
            from.filterNot(_.trim.isEmpty).map(UnvalidatedPhoneNumber(_)),
            UnvalidatedPhoneNumber(to),
            body
          )
        } yield TextMessageRequest(txt)

    }

  def getService(
    texts: Texts[F],
    proxies: logic.Proxies[F],
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

    case req @ GET -> Root / "text-replies" :? MessageSidMatcher(msid) +& FromMatcher(
          from
        ) +& ToMatcher(to) +& BodyMatcher(body) => {
      val inbound = InboundTwilioText(msid, from, to, body)
      for {
        _ <- Logger[F].info(
              s"RECEIVED THE FOLLOWING msid: $msid, from: $from, to: $to, body: $body"
            )
        _ <- proxies
              .startProxy(inbound)
              .map(_ => ())
              .recover(errorHandler.recovering)
        _ <- Logger[F].info("kicked off the proxy proces")
        //by replying 204 no content we are telling twilio that every is
        //good and we dont need it to do anything else for us
        //if we respond 200 it will expect there to be TWIML (twilio xml)
        // in the body of the response and will show errors in the console debugger when
        //there is not
        resp <- NoContent()
      } yield resp
    }
  }

}

object ParamMatchers {

  object MessageSidMatcher
      extends QueryParamDecoderMatcher[String]("MessageSid")
  object FromMatcher extends QueryParamDecoderMatcher[String]("From")
  object ToMatcher   extends QueryParamDecoderMatcher[String]("To")
  object BodyMatcher extends QueryParamDecoderMatcher[String]("Body")
}
