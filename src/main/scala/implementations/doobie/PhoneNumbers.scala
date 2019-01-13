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
  proxiesRepo: ProxiesRepository[Task],
  transactorProvider: TransactorProvider[Task]
) extends PhoneNumbersRepository[Task] {
  import PhoneNumbersDoobie._
  import DoobieUtils.execute
  import DoobieUtils.scheduleExecution

  implicit val tp = transactorProvider

  def getAll: Task[IList[TwilioNumber]] =
    Task.point(IList.empty)

  def getAllAvailable: Task[IList[TwilioNumber]] =
    execute[List[TwilioNumber]](getNotInUse).map(_.toIList)

  def getAvailable: Task[Option[TwilioNumber]] =
    execute[Option[TwilioNumber]](getAnySingleNotInUse)

  def save(ph: TwilioNumber): Task[TwilioNumber] =
    execute[TwilioNumber](for {
      _ <- insert(ph).run
      n <- findByTwilio(ph.num)
    } yield n.get)

  def saveAll(phs: NonEmptyList[TwilioNumber]): Task[Int] =
    execute[Int](insertAll(phs))

  def delete(ph: TwilioNumber): Task[Unit] = Task.point(())

  def find(uuid: UUID): Task[Option[TwilioNumber]] = ???

  def findByTwilioNumber(tn: String): Task[Option[TwilioNumber]] =
    execute[Option[TwilioNumber]](findByTwilio(tn))

  def findByNumberSid(sid: String): Task[Option[TwilioNumber]] =
    execute[Option[TwilioNumber]](findBySid(sid))

  def setUnavailableFor(
    ph: TwilioNumber,
    until: Epoch
  ): Task[Unit] =
    execute[Int](
      update(ph.copy(inUse = true, willBeAvailable = Some(until))).run
    ).map(_ => ())

  def setAvailable(ph: TwilioNumber): Task[Unit] =
    execute[Int](update(ph.copy(inUse = false, willBeAvailable = None)).run)
      .map(_ => ())

  def scheduleSetAvailable(ph: TwilioNumber, d: Duration): Task[Unit] = {
    val q: ConnectionIO[Unit] = for {
      ppActive <- ProxiesDoobie
                   .findByTwilio(ph.num)
                   .map(_.exists(_.info.isDefined))
      _ <- if (!ppActive)
            update(ph.copy(inUse = false, willBeAvailable = None)).run
          else connection.unit
    } yield ()
    scheduleExecution[Unit](q, d)
  }
}

object PhoneNumbersDoobie {
  import DoobieUtils._

  def apply(
    ppr: ProxiesRepository[Task],
    tp: TransactorProvider[Task]
  ): PhoneNumbersDoobie =
    new PhoneNumbersDoobie(ppr, tp)

  def insert(tn: TwilioNumber) =
    sql"""insert into TwilioPhoneNumbers (sid, number, inUse) values (${tn.sid}, ${tn.num}, ${tn.inUse})""".update

  def insertAll(tns: NonEmptyList[TwilioNumber]): ConnectionIO[Int] = {
    val sql =
      "insert into TwilioPhoneNumbers (sid, number, inUse) values (?, ?, ?)"
    val tuples: List[(String, String, Boolean)] =
      tns.map(n => (n.sid, n.num, n.inUse)).toList
    Update[(String, String, Boolean)](sql).updateMany(tuples)
  }

  def update(tn: TwilioNumber) =
    sql"""update TwilioPhoneNumbers set inUse = ${tn.inUse} , willBeAvailable = ${tn.willBeAvailable} where sid = ${tn.sid}""".update

  def findByUUID(uuid: UUID) =
    sql"""select sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where uuid = $uuid"""
      .query[TwilioNumber]
      .option

  def findByTwilio(tn: String) =
    sql"""select sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where number = $tn"""
      .query[TwilioNumber]
      .option

  def findBySid(sid: String) =
    sql"""select sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where sid = $sid"""
      .query[TwilioNumber]
      .option

  def getNotInUse =
    sql"select sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where inUse = 'false'"
      .query[TwilioNumber]
      .to[List]

  def getAnySingleNotInUse =
    sql"select 1 sid, number, inUse, dateCreated, willBeAvailable, uuid from TwilioPhoneNumbers where inUse = 'false'"
      .query[TwilioNumber]
      .option
}
