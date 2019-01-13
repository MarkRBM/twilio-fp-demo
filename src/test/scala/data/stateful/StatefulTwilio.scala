package tests.data.statefulimplementations

import interfaces._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.noop.NoOpLogger
import java.util.UUID
import logic.{ PhoneNumbers, Proxies, Texts }
import scala.concurrent.duration._
import simulacrum._
import scalaz._
import Scalaz._
import scalaz.zio.interop.Task
import shims.effect._
import cats.effect.Sync
import cats.implicits._
import utilities._
import models._
import tests.utilities._

object StatefulTwilio {
  import Helpers._
  import State.{ get, modify }
  import StateT._

  def apply: Twilio[F] = new Twilio[F] {
    def sendSMS(
      twilioNumber: TwilioNumber,
      msg: TwilioSMS
    ): F[Unit] = get.map(_ => ())
    def sendMMS(
      twilioNumber: TwilioNumber,
      msg: TwilioMMS
    ): F[Unit] = ???
    def toTwilioTextMessage(msg: ValidatedTextMessage): F[TwilioTextMessage] =
      get.map(_ => TwilioSMS(msg.from, msg.to, msg.body))

    def validateNumber(num: PhoneNumber): F[ValidatedPhoneNumber] =
      get.map(_ => ValidatedPhoneNumber(num.num))

    def getNewNumber: F[TwilioNumber] =
      createNewNumber

    private def createNewNumber(
      implicit F: MonadState[F, World]
    ): F[TwilioNumber] =
      for {
        world <- F.get
        newNumber = TwilioNumber(
          "NewTestTwilioNumberSID",
          "NewTestTwilioNumber",
          false,
          Some(Epoch(1L)),
          None,
          None
        )
      } yield newNumber

    def startProxy(
      in: Proxy,
      initialTextBody: String
    ): F[Option[ProxyInfo]] =
      start(in)
    private def start(in: Proxy)(
      implicit F: MonadState[F, World]
    ): F[Option[ProxyInfo]] =
      for {
        world         <- F.get
        newProxyId    = Some(ProxyInfo("NewTestProxyInfo"))
        newProxyState = in.copy(info = newProxyId)
        _             <- F.put(world.copy(pendingProxies = IList(newProxyState)))
      } yield newProxyId

    def endProxy(in: Proxy): F[Unit] = removeProxy(in)

    private def removeProxy(in: Proxy)(
      implicit F: MonadState[F, World]
    ) =
      for {
        world             <- F.get
        withOutEndedProxy = world.pendingProxies.filterNot(_.uuid == in.uuid)
        newWorld          = world.copy(pendingProxies = withOutEndedProxy)
        _                 <- F.put(world)
      } yield ()

    def scheduleProxyEnd(
      in: Proxy,
      d: Duration,
      afterEnding: (Proxy) => F[Unit]
    ): F[Unit] = get.map(_ => ())
  }

}
