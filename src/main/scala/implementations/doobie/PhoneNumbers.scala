package implementations.doobs

import doobie.free.connection
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._

import scalaz._
import Scalaz._
import scalaz.zio.IO
import scalaz.zio.interop._
import scalaz.zio.interop.Task
import scalaz.zio.interop.scalaz72._
import scalaz.zio.interop.catz._

import cats._
import cats.implicits._

import doobie._
import doobie.implicits._

import interfaces._
import utilities._
import models._
import doobie.Types
import doobie.free.Types
import doobie.free.connection.ConnectionIO
import models._
import scala.`package`.List

final class PhoneNumbersDoobie(
  transactorProvider: TransactorProvider[Task]
) extends PhoneNumbersRepository[Task] {
  import PhoneNumbersDoobie._
  import DoobieUtils.execute

  implicit val tp = transactorProvider

  def getAvailable: Task[Option[TwilioNumber]] =
    execute[Option[TwilioNumber]](getAnySingleNotInUse)
}

object PhoneNumbersDoobie {
  import DoobieUtils._

  def apply(
    tp: TransactorProvider[Task]
  ): PhoneNumbersDoobie =
    new PhoneNumbersDoobie(tp)
  def getAnySingleNotInUse =
    sql"select sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where inUse = 'false'"
      .query[TwilioNumber]
      .option
}
