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
      IList.empty,
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
      IList.empty,
      IList(twilioNumber)
    )

    val (newWorld, phoneNumber) =
      phoneNumbers.getTwilioNumber.run(initialWorld)

    newWorld.shouldBe(expectedWorld)
    phoneNumber.shouldBe(twilioNumber)
  }
  it should "should be able to buy a new number for a specific customer if necessary" in {
    val createdDate = Some(Epoch(1L))
    val initialWorld = World(
      IList.empty,
      IList.empty
    )
    val expectedNumber = TwilioNumber(
      "NewTestTwilioNumberSID",
      "NewTestTwilioNumber",
      false,
      createdDate,
      None,
      None
    )
    val expectedWorld = World(
      IList.empty,
      IList(expectedNumber)
    )

    val (newWorld, phoneNumber) =
      phoneNumbers.getTwilioNumber.run(initialWorld)

    newWorld.shouldBe(expectedWorld)
    phoneNumber.shouldBe(expectedNumber)
  }
  it should "be able to set that number to be in use" in {
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
      IList.empty,
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
    val unavailableUntil = Epoch(2L)
    val inUseNumber =
      twilioNumber.copy(inUse = true, willBeAvailable = Some(unavailableUntil))
    val expectedWorld = World(
      IList.empty,
      IList(inUseNumber)
    )

    val (newWorld, _) = phoneNumbers
      .setInUse(twilioNumber, unavailableUntil)
      .run(initialWorld)

    newWorld.shouldBe(expectedWorld)
  }
  it should "be able to set a number back to being available" in {
    val createdDate      = Some(Epoch(1L))
    val unavailableUntil = Epoch(2L)
    val twilioNumber = TwilioNumber(
      "TestTwilioNumberSID",
      "TestTwilioNumber",
      true,
      createdDate,
      Some(unavailableUntil),
      None
    )
    val initialWorld = World(
      IList.empty,
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
    val availableNumber =
      twilioNumber.copy(inUse = false, willBeAvailable = None)
    val expectedWorld = World(
      IList.empty,
      IList(availableNumber)
    )

    val (newWorld, phoneNumber) = phoneNumbers
      .setAvailable(twilioNumber)
      .run(initialWorld)

    newWorld.shouldBe(expectedWorld)
  }
}
