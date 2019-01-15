package logic

import io.chrisdavenport.log4cats.Logger
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
      //dont do this .get
      toBeReturned <- phoneNumbersRepo.getAvailable.map(_.get)
    } yield toBeReturned
}
