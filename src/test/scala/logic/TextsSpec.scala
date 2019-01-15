package tests.logic

import models._
import logic.PhoneNumbers
import logic.Texts
import scalaz._
import Scalaz._

import org.scalatest._
import utilities.Epoch
import tests.data.statefulimplementations._
import tests.utilities._
import tests.utilities.Helpers.F
import interfaces.Twilio

class TextsSpec extends FlatSpec with Matchers {
  import Helpers._

  val twilio: Twilio[F] = StatefulTwilio.apply
  val phoneNumbers: PhoneNumbers[F] =
    new PhoneNumbers[F](StatefulPhoneNumbers.apply, twilio)

  val textsProgram: Texts[F] =
    new Texts[F](twilio, phoneNumbers, mockConf)
  "Texts logic" should "be able to send a text message" in {
    val msg = SMS(
      None,
      UnvalidatedPhoneNumber("toNumber"),
      "test text Body"
    )
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
    val expectedWorld = initialWorld
    val (newWorld, result) =
      textsProgram.sendText(msg).run(initialWorld)

    newWorld.shouldBe(expectedWorld)
  }
}
