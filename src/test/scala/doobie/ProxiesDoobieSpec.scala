package tests.models

import java.util.UUID
import implementations.doobs.ProxiesDoobie
import models._
import org.scalatest._

import tests.data.statefulimplementations._
import tests.utilities._

class ProxiesRepoSpec extends FlatSpec with Matchers {
  import Helpers._

  //we are ignoring the autgenerated uuids in each assertion deliberately
  "Pending Proxy Repo" should
    "be able to insert a new pending proxie and retrieve it again" in new InMemoryDB {
    val newPP: Proxy =
      Proxy(
        "someNumber",
        "someOtherNumber",
        "someTwilioNumber",
        None,
        None
      )
    val pendingProxies: ProxiesDoobie = new ProxiesDoobie(tp)

    val id: UUID                   = unsafeRun(pendingProxies.save(newPP))
    val retrievedPP: Option[Proxy] = unsafeRun(pendingProxies.find(id))

    retrievedPP.map(_.copy(uuid = None)).shouldBe(Some(newPP))
  }
  it should "be able to find a pending proxy by from and twilio" in new InMemoryDB {
    val newPP: Proxy =
      Proxy(
        "someNumber",
        "someOtherNumber",
        "someTwilioNumberTwo",
        None,
        None
      )
    val pendingProxies: ProxiesDoobie = new ProxiesDoobie(tp)
    val id: UUID                      = unsafeRun(pendingProxies.save(newPP))

    val retrievedPP: Option[Proxy] =
      unsafeRun(pendingProxies.find(newPP.to, newPP.twilioNumber))

    retrievedPP.map(_.copy(uuid = None)).shouldBe(Some(newPP))
  }
  it should "be able to find a pending proxy by twilio number" in new InMemoryDB {
    val newPP: Proxy =
      Proxy(
        "someNumber",
        "someOtherNumber",
        "someTwilioNumberThree",
        None,
        None
      )
    val pendingProxies: ProxiesDoobie = new ProxiesDoobie(tp)
    val id: UUID                      = unsafeRun(pendingProxies.save(newPP))

    val retrievedPP: Option[Proxy] =
      unsafeRun(pendingProxies.findByTwilioNumber(newPP.twilioNumber))

    retrievedPP.map(_.copy(uuid = None)).shouldBe(Some(newPP))
  }
  it should "be able to update an existing pending proxy" in new InMemoryDB {
    val newPP: Proxy =
      Proxy(
        "someNumber",
        "someOtherNumber",
        "someTwilioNumberThree",
        None,
        None
      )
    val pendingProxies: ProxiesDoobie = new ProxiesDoobie(tp)
    val id: UUID                      = unsafeRun(pendingProxies.save(newPP))

    val retrievedPP: Option[Proxy] =
      unsafeRun(pendingProxies.findByTwilioNumber(newPP.twilioNumber))
    val updatedPP: Option[Proxy] =
      retrievedPP.map(_.copy(info = Some(ProxyInfo("someNewProxyId"))))
    unsafeRun(pendingProxies.update(updatedPP.get))

    val retrievedUpdatedPP: Option[Proxy] =
      unsafeRun(pendingProxies.findByTwilioNumber(newPP.twilioNumber))
    retrievedUpdatedPP.shouldBe(updatedPP)
  }
  it should "allow you to delete a pp" in new InMemoryDB {
    val newPP: Proxy =
      Proxy(
        "someNumber",
        "someOtherNumber",
        "someTwilioNumberThree",
        None,
        None
      )
    val pendingProxies: ProxiesDoobie = new ProxiesDoobie(tp)
    val id: UUID                      = unsafeRun(pendingProxies.save(newPP))

    val retrievedPP: Option[Proxy] =
      unsafeRun(pendingProxies.findByTwilioNumber(newPP.twilioNumber))

    retrievedPP.map(_.copy(uuid = None)).shouldBe(Some(newPP))

    unsafeRun(pendingProxies.delete(newPP))

    val retrievedAgain: Option[Proxy] =
      unsafeRun(pendingProxies.findByTwilioNumber(newPP.twilioNumber))

    retrievedAgain.shouldBe(None)
  }
  it should "prevent you adding a row if twilio number already in the db and give you a nice error" in pending
}
