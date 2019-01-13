package tests.doobie

import implementations.doobs.ProxiesDoobie
import scala.concurrent.duration._

import scalaz._
import Scalaz._

import implementations.doobs.PhoneNumbersDoobie
import org.scalatest._

import utilities._
import tests.data.statefulimplementations._
import tests.utilities._
import scala.collection.Seq
import scalaz.IList

import models._

class PhoneNumbersDoobieSpec extends FlatSpec with Matchers {
  import Helpers._

  val testNumbers: Seq[TwilioNumber] = Seq(
    TwilioNumber("sidOne", "oneNumber", false, None, None, None),
    TwilioNumber("sidTwo", "twoNumber", false, None, None, None),
    TwilioNumber("sidThree", "threeNumber", false, None, None, None),
    TwilioNumber("sidFour", "fourNumber", false, None, None, None)
  )

  "PhoneNumbers" should
    "be able to insert a new phone number and retrieve it again" in new InMemoryDB {

    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    val num: TwilioNumber = unsafeRun(
      phoneNumbers.save(testNumbers.head)
    )
    rollBackTest()
    num.sid.shouldBe(testNumbers.head.sid)
  }
  it should "be able to find a phone number by twilio number" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    unsafeRun(
      phoneNumbers.saveAll(
        NonEmptyList(testNumbers.head, testNumbers.tail.head)
      )
    )
    val retrieved: Option[TwilioNumber] = unsafeRun(
      phoneNumbers.findByTwilioNumber(testNumbers.tail.head.num)
    )
    rollBackTest()
    retrieved.map(_.sid).shouldBe(Some(testNumbers.tail.head.sid))
  }
  it should "be able to find a phone number by sid" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    unsafeRun(
      phoneNumbers.saveAll(
        NonEmptyList(testNumbers.head, testNumbers(2))
      )
    )
    val retrieved: Option[TwilioNumber] = unsafeRun(
      phoneNumbers.findByNumberSid(testNumbers(2).sid)
    )
    rollBackTest()
    retrieved.map(_.sid).shouldBe(Some(testNumbers(2).sid))
  }
  it should "be able to return all available numbers" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    unsafeRun(
      phoneNumbers.saveAll(
        NonEmptyList(
          testNumbers(0),
          testNumbers(1),
          testNumbers(2).copy(inUse = true),
          testNumbers(3)
        )
      )
    )

    val retrieved: IList[TwilioNumber] = unsafeRun(
      phoneNumbers.getAllAvailable
    )
    rollBackTest()
    retrieved.length.shouldBe(3)
  }
  it should "be able to return a single available number" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    unsafeRun(
      phoneNumbers.saveAll(
        NonEmptyList(
          testNumbers(0),
          testNumbers(1),
          testNumbers(2).copy(inUse = true),
          testNumbers(3)
        )
      )
    )

    val retrieved: Option[TwilioNumber] = unsafeRun(
      phoneNumbers.getAvailable
    )
    rollBackTest()
    List("sidOne", "sidTwo", "sidfour")
      .contains(retrieved.map(_.sid).get)
      .shouldBe(true)
  }
  it should "be able to make a number unavailable" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(new ProxiesDoobie(tp), tp)
    unsafeRun(
      phoneNumbers.saveAll(
        NonEmptyList(
          testNumbers(0),
          testNumbers(1),
          testNumbers(2).copy(inUse = true),
          testNumbers(3)
        )
      )
    )
    //make sidTwo Unavailable
    unsafeRun(
      phoneNumbers
        .setUnavailableFor(testNumbers(1), (Epoch.now + (5 minutes)))
    )

    val retrieved: IList[TwilioNumber] = unsafeRun(
      phoneNumbers.getAllAvailable
    )
    rollBackTest()
    retrieved.map(_.sid).toList.contains("sidTwo").shouldBe(false)
  }
  it should "be able to return all unavailable numbers" in pending
  it should "prevent you adding a row if twilio number already in the db and give you a nice error" in pending
}
