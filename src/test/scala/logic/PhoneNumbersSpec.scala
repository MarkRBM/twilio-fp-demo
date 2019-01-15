package tests.logic

import models._
import logic.PhoneNumbers
import scalaz._
import Scalaz._

import org.scalatest._

import tests.data._
import tests.utilities.Helpers.World
import utilities.Epoch
import tests.data.statefulimplementations._
import tests.utilities._
import tests.utilities.Helpers.F
import interfaces.Twilio

class PhoneNumbersSpec extends FlatSpec with Matchers {
  import Helpers._

  val twilio: Twilio[F] = StatefulTwilio.apply
  val phoneNumbers: PhoneNumbers[F] =
    new PhoneNumbers[F](StatefulPhoneNumbers.apply, twilio)

  "Phone numbers logic" should "return an available twilio number" in {
    val createdDate = Some(Epoch(1L))
    val twilioNumber = TwilioNumber(
      "TestTwilioNumberSID",
      "TestTwilioNumber",
      false,
      createdDate,
      None,
      None
    )
    val initialWorld = World(
      IList(
        TwilioNumber(
          "TestTwilioNumberSID",
          "TestTwilioNumber",
          false,
          createdDate,
          None,
          None
        )
      )
    )
    val expectedWorld = World(
      IList(twilioNumber)
    )

    val (newWorld, phoneNumber) =
      phoneNumbers.getTwilioNumber.run(initialWorld)

    newWorld.shouldBe(expectedWorld)
    phoneNumber.shouldBe(twilioNumber)
  }
}
