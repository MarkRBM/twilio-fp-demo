package tests.logic

import models._
import logic.Proxies
import logic.PhoneNumbers
import scalaz._
import Scalaz._

import org.scalatest._

import tests.data.statefulimplementations._
import tests.utilities._
import tests.utilities.Helpers.F
import interfaces.Twilio

class ProxiesSpec extends FlatSpec with Matchers {
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

  "Proxies logic" should "be able to start a pending proxy" in {
    val inboundText = InboundTwilioText(
      "testMessageSid",
      "hostNumber",
      "twilioNumber",
      "Inbound text body"
    )
    val initialWorld = World(
      IList(
        Proxy("visitorNumber", "hostNumber", "twilioNumber", None, None)
      ),
      IList.empty
    )

    val expectedWorld = World(
      IList(
        Proxy(
          "visitorNumber",
          "hostNumber",
          "twilioNumber",
          Some(ProxyInfo("NewTestProxyInfo")),
          None
        )
      ),
      IList.empty
    )
    val expectedProxyInfo = Some(ProxyInfo("NewTestProxyInfo"))

    val (newWorld, proxyInfo) = proxies
      .startProxy(inboundText)
      .run(initialWorld)

    newWorld.shouldBe(expectedWorld)
    proxyInfo.shouldBe(expectedProxyInfo)
  }
  it should "stop the creation of a proxy and return None" in {
    val inboundText = InboundTwilioText(
      "testMessageSid",
      "hostPhoneNumber",
      "twilioPhoneNumber",
      "Inbound text body"
    )
    val initialWorld = World(
      IList.empty,
      IList.empty
    )

    val (newWorld, proxyInfo) = proxies
      .startProxy(inboundText)
      .run(initialWorld)

    newWorld.shouldBe(initialWorld)
    proxyInfo.shouldBe(None)
  }
}
