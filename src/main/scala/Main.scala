package main

import fs2.Stream
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import implementations.kafka.SmsNotificationsBroker
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import java.util.UUID
import logic.kafka.SmsNotifications
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder
import pureconfig.{ CamelCase, ConfigFieldMapping, ProductHint }

import scalaz.zio.interop._
import scalaz.zio.interop.scalaz72._
import scalaz.zio.interop.catz._

import scala.concurrent.ExecutionContext

import doobie._
import doobie.h2._
import doobie.h2.implicits._
import doobie.implicits._

import implementations._
import logic._
import http._
import implementations.TwilioImpl
import implementations.doobs._
import logic.{ PhoneNumbers, Texts }
import scalaz.zio.interop.`package`.Task
import utilities.{ KafkaConfig, ServerConfig }
import implementations.RaygunErrorHandler
import implementations.doobs.DoobieTransactorProvider
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import org.http4s.client.Client

object Main extends StreamApp[Task] {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def unsafeLogger: SelfAwareStructuredLogger[Task] =
    Slf4jLogger.unsafeCreate[Task]

  val httpClient: Stream[Task, Client[Task]] = Http1Client.stream[Task]()

  implicit val serverConfHint: ProductHint[ServerConfig] =
    ProductHint[ServerConfig](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val kafkaConfHint: ProductHint[KafkaConfig] =
    ProductHint[KafkaConfig](ConfigFieldMapping(CamelCase, CamelCase))

  val conf: ServerConfig               = pureconfig.loadConfigOrThrow[ServerConfig]
  val errorHandler: RaygunErrorHandler = new RaygunErrorHandler(conf)
  val twilio: TwilioImpl               = TwilioImpl.apply(conf)
  val tp: DoobieTransactorProvider     = new DoobieTransactorProvider()
  val broker: SmsNotificationsBroker   = new SmsNotificationsBroker(conf)

  override def stream(
    args: List[String],
    requestShutdown: Task[Unit]
  ): Stream[Task, ExitCode] =
    httpClient.flatMap(client => {
      val pendingProxiesRepo = ProxiesDoobie(tp)

      val phoneNumberRepo: PhoneNumbersDoobie =
        PhoneNumbersDoobie(pendingProxiesRepo, tp)

      val phoneNumbers: PhoneNumbers[Task] =
        new PhoneNumbers[Task](phoneNumberRepo, twilio)

      val proxies =
        new logic.Proxies[Task](twilio, pendingProxiesRepo, phoneNumbers, conf)

      val txts: Texts[Task] =
        new Texts[Task](twilio, phoneNumbers, proxies, conf)

      //lets not set up kafka for the demo but just uncomment here to do it
      // val kafkaStream =
      //   new SmsNotifications[Task](broker, txts, errorHandler).stream

      // Stream(
      TextHttpServer.httpStream(txts, proxies, errorHandler)
      // kafkaStream.drain
      // ).join(2)
    })
}
