package logic

import io.chrisdavenport.log4cats.Logger
import scala.concurrent.duration.{ Duration, FiniteDuration }
import simulacrum._
import scalaz._
import Scalaz._

import utilities._
import interfaces._
import models._

final class PhoneNumbers[F[_]](
  phoneNumbersRepo: PhoneNumbersRepository[F],
  twilio: Twilio[F]
)(implicit F: Monad[F], L: Logger[F]) {
  def getTwilioNumber: F[TwilioNumber] =
    for {
      _ <- Logger[F].info(
            s"Trying to get an available number from the db"
          )
      maybeNumber <- phoneNumbersRepo.getAvailable
      toBeReturned <- maybeNumber.map(F.pure(_)).getOrElse {
                       for {
                         _ <- Logger[F].info(
                               s"No available Number in DB getting a new one. "
                             )
                         newNumber   <- twilio.getNewNumber
                         _           <- Logger[F].info(s"Got new number ${newNumber.num}")
                         savedNumber <- phoneNumbersRepo.save(newNumber)
                         _           <- Logger[F].info(s"Saved it to the db")
                       } yield savedNumber
                     }
    } yield toBeReturned

  //also set a time to free up the number
  def setInUse(ph: TwilioNumber, until: Epoch): F[Unit] =
    Logger[F].info(s"Setting $ph to in use until $until") *>
      phoneNumbersRepo.setUnavailableFor(ph, until)

  def setAvailable(ph: TwilioNumber): F[Unit] =
    Logger[F].info(s"Setting $ph to available") *>
      phoneNumbersRepo.setAvailable(ph)

  def scheduleSetAvailable(
    ph: TwilioNumber,
    d: Duration
  ): F[Unit] =
    Logger[F].info(s"scheduling $ph to be available in $d") *>
      phoneNumbersRepo.scheduleSetAvailable(ph, d)

  def find(twilioNumber: String): F[Option[TwilioNumber]] =
    phoneNumbersRepo.findByTwilioNumber(twilioNumber)
}
