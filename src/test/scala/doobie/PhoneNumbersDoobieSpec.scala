package tests.doobie

import scala.concurrent.duration._

import scalaz._
import Scalaz._
import scalaz.zio.interop._
import scalaz.zio.interop.Task
import scalaz.zio.interop.scalaz72._
import scalaz.zio.interop.catz._

import cats._
import cats.implicits._

import implementations.doobs.TransactorProvider
import implementations.doobs.PhoneNumbersDoobie
import org.scalatest._

import utilities._
import tests.data.statefulimplementations._
import tests.utilities._
import scala.collection.Seq
import scalaz.IList

import doobie._
import doobie.implicits._
import doobie.Types
import doobie.free.Types
import doobie.free.connection.ConnectionIO
import DoobieUtils.execute

import models._

class PhoneNumbersDoobieSpec extends FlatSpec with Matchers {
  import Helpers._

  val testNumbers: Seq[TwilioNumber] = Seq(
    TwilioNumber("sidOne", "oneNumber", false, None, None, None),
    TwilioNumber("sidTwo", "twoNumber", false, None, None, None),
    TwilioNumber("sidThree", "threeNumber", false, None, None, None),
    TwilioNumber("sidFour", "fourNumber", false, None, None, None)
  )
  def insertAll(tns: NonEmptyList[TwilioNumber]): ConnectionIO[Int] = {
    val sql =
      "insert into TwilioPhoneNumbers (sid, number, inUse) values (?, ?, ?)"
    val tuples: List[(String, String, Boolean)] =
      tns.map(n => (n.sid, n.num, n.inUse)).toList
    Update[(String, String, Boolean)](sql).updateMany(tuples)
  }

  def saveAll(phs: NonEmptyList[TwilioNumber])(implicit tp: TransactorProvider[Task]): Task[Int] =
    DoobieUtils.execute[Int](insertAll(phs))

  "PhoneNumbers" should "be able to return a single available number" in new InMemoryDB {
    val phoneNumbers: PhoneNumbersDoobie =
      new PhoneNumbersDoobie(tp)
    unsafeRun(
      saveAll(
        NonEmptyList(
          testNumbers(0)
          // testNumbers(1),
          // testNumbers(2).copy(inUse = true),
          // testNumbers(3)
        )
      )(tp)
    )

    val retrieved: Option[TwilioNumber] = unsafeRun(
      phoneNumbers.getAvailable
    )
    rollBackTest()
    retrieved.map(_.sid) shouldBe Some("sidOne")
  }
}
