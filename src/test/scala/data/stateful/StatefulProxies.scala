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

object StatefulProxies {
  import Helpers._
  import State.{ get, modify }
  import StateT._

  def apply: ProxiesRepository[F] = new ProxiesRepository[F] {
    def find(from: String, twilioNumber: String): F[Option[Proxy]] =
      get.map(
        _.pendingProxies
          .find(pp => pp.to == from && pp.twilioNumber == twilioNumber)
      )
    def find(uuid: UUID): F[Option[Proxy]] = ???
    def findByTwilioNumber(tn: String): F[Option[Proxy]] =
      ???
    def save(p: Proxy): F[UUID] = savePP(p)

    private def savePP(
      p: Proxy
    )(implicit F: MonadState[F, World]): F[UUID] =
      for {
        world    <- F.get
        newWorld = world.copy(pendingProxies = world.pendingProxies :+ p)
        _        <- F.put(newWorld)
      } yield UUID.randomUUID()
    def update(p: Proxy): F[Proxy] = updatePP(p)

    private def updatePP(p: Proxy)(
      implicit F: MonadState[F, World]
    ) =
      for {
        world <- F.get
        toBeUpdated = world.pendingProxies
          .find(_.twilioNumber == p.twilioNumber)
          .get
        updated = toBeUpdated.copy(info = p.info)
        newWorld = world.copy(
          pendingProxies = (world.pendingProxies
            .filterNot(_.twilioNumber == p.twilioNumber) :+ updated)
        )
        _ <- F.put(newWorld)
      } yield updated

    def delete(p: Proxy): F[Unit] = ???
    def scheduleDelete(p: Proxy, d: Duration): F[Unit] =
      get.map(_ => ())
  }

}
