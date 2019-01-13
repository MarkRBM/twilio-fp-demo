package tests.logic

import models._
import logic.Proxies
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
  val proxies: Proxies[F] = new Proxies[F](
    StatefulTwilio.apply,
    StatefulProxies.apply,
    phoneNumbers,
    mockConf
  )

  val textsProgram: Texts[F] =
    new Texts[F](twilio, phoneNumbers, proxies, mockConf)
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
    val expectedWorld = initialWorld
    val (newWorld, result) =
      textsProgram.sendText(msg).run(initialWorld)

    newWorld.shouldBe(expectedWorld)
  }
  it should "add a pending proxy and set the number to inUse if allowProxy = true" in {
    val msg = SMS(
      Some(UnvalidatedPhoneNumber("fromNumber")),
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
      IList(
        Proxy(msg.from.get.num, msg.to.num, twilioNumber.num, None, None)
      ),
      //the Some(Epoch.now) value here is not easily testable and so is being diregarded in the assertions
      IList(
        TwilioNumber(
          "TestTwilioNumberSID",
          "TestTwilioNumber",
          true,
          createdDate,
          Some(Epoch.now),
          None
        )
      )
    )
    val (newWorld, result) =
      textsProgram.sendText(msg).run(initialWorld)
    newWorld.shouldBe(expectedWorld)
    newWorld.pendingProxies.length.shouldBe(1)
    newWorld.twilioPhoneNumbers.length.shouldBe(1)
    newWorld.twilioPhoneNumbers.headOption.map(_.inUse).shouldBe(Some(true))
  }
}
