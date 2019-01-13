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

object StatefulPhoneNumbers {
  import Helpers._
  import State.{ get, modify }
  import StateT._

  def apply: PhoneNumbersRepository[F] = new PhoneNumbersRepository[F] {
    def getAll: F[IList[TwilioNumber]]          = ???
    def getAllAvailable: F[IList[TwilioNumber]] = ???
    def getAvailable: F[Option[TwilioNumber]] =
      get.map(w => w.twilioPhoneNumbers.find(_.inUse == false))
    def save(ph: TwilioNumber): F[TwilioNumber] =
      saveAndReturnNumber(ph)
    def saveAll(phs: NonEmptyList[TwilioNumber]): F[Int] = ???
    def delete(ph: TwilioNumber): F[Unit]                = ???
    def find(uuid: UUID): F[Option[TwilioNumber]]        = ???
    def findByNumberSid(sid: String): F[Option[TwilioNumber]] =
      ???
    def findByTwilioNumber(tn: String): F[Option[TwilioNumber]] =
      ???
    def setUnavailableFor(
      ph: TwilioNumber,
      until: Epoch
    ): F[Unit] = makeUnavailable(ph, until)
    def setAvailable(ph: TwilioNumber): F[Unit] =
      makeAvailable(ph)

    def scheduleSetAvailable(ph: TwilioNumber, d: Duration): F[Unit] =
      get.map(_ => ())

    private def saveAndReturnNumber(
      ph: TwilioNumber
    )(implicit F: MonadState[F, World]): F[TwilioNumber] =
      for {
        world <- F.get
        _ <- F.put(
              world.copy(twilioPhoneNumbers = world.twilioPhoneNumbers :+ ph)
            )
      } yield ph
    private def makeUnavailable(ph: TwilioNumber, until: Epoch)(
      implicit F: MonadState[F, World]
    ): F[Unit] =
      for {
        world <- F.get
        unavailableNumber = ph.copy(
          inUse = true,
          willBeAvailable = Some(until)
        )
        removeOldNumber = world.copy(
          twilioPhoneNumbers = world.twilioPhoneNumbers.filterNot(_ == ph)
        )
        addNewNumber = removeOldNumber.copy(
          twilioPhoneNumbers = removeOldNumber.twilioPhoneNumbers :+ unavailableNumber
        )
        _ <- F.put(addNewNumber)
      } yield ()
    private def makeAvailable(
      ph: TwilioNumber
    )(implicit F: MonadState[F, World]): F[Unit] =
      for {
        world <- F.get
        removeUnavailable = world.copy(
          twilioPhoneNumbers =
            world.twilioPhoneNumbers.filterNot(_.sid == ph.sid)
        )
        addAvailable = removeUnavailable.copy(
          twilioPhoneNumbers = removeUnavailable.twilioPhoneNumbers :+ ph
            .copy(inUse = false, willBeAvailable = None)
        )
        _ <- F.put(addAvailable)
      } yield ()
  }

}
