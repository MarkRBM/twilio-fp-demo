package implementations.doobs

import doobie.free.connection
import java.util.UUID
import scala.concurrent.duration.Duration

import scalaz._
import Scalaz._
import scalaz.zio.IO
import scalaz.zio.interop._
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

import doobie._
import doobie.implicits._

import interfaces._
import models._
import utilities._

final class ProxiesDoobie(
  transactorProvider: TransactorProvider[Task]
) extends ProxiesRepository[Task] {
  import ProxiesDoobie._
  import DoobieUtils.execute
  import DoobieUtils.scheduleExecution

  implicit val tp = transactorProvider

  def find(to: String, twilioNumber: String): Task[Option[Proxy]] =
    execute[Option[Proxy]](findByToAndTwilio(to, twilioNumber))

  def find(uuid: UUID): Task[Option[Proxy]] =
    execute[Option[Proxy]](findByUUID(uuid))

  def findByTwilioNumber(tn: String): Task[Option[Proxy]] =
    execute[Option[Proxy]](findByTwilio(tn))

  def save(p: Proxy): Task[UUID] =
    execute[UUID](for {
      id    <- insert(p).run
      newPP <- findByTwilio(p.twilioNumber)
    } yield newPP.flatMap(_.uuid).get)

  def update(p: Proxy): Task[Proxy] =
    execute[Proxy](for {
      _     <- setInfo(p.twilioNumber, p.info).run
      newPP <- findByTwilio(p.twilioNumber)
    } yield newPP.get)

  def delete(p: Proxy): Task[Unit] =
    execute[Int](deletePP(p.twilioNumber).run).map(_ => ())

  def scheduleDelete(p: Proxy, d: Duration): Task[Unit] = {
    val q = for {
      ppActive <- findByTwilio(p.twilioNumber).map(_.exists(_.info.isDefined))
      res      <- if (!ppActive) deletePP(p.twilioNumber).run else connection.pure(0)
    } yield ()
    scheduleExecution[Unit](q, d)
  }
}

object ProxiesDoobie {
  import DoobieUtils._

  def apply(
    tp: TransactorProvider[Task]
  ): ProxiesDoobie =
    new ProxiesDoobie(tp)

  def insert(p: Proxy) =
    sql"""insert into TwilioProxies ("from", "to", twilioNumber) values (${p.from}, ${p.to}, ${p.twilioNumber})""".update

  def setInfo(twilioNumber: String, info: Option[ProxyInfo]) =
    sql"""update TwilioProxies set info = $info where twilioNumber = $twilioNumber""".update

  def deletePP(twilioNumber: String) =
    sql"""delete from TwilioProxies where twilioNumber = $twilioNumber""".update

  def findByToAndTwilio(to: String, tn: String) =
    sql"""select "from", "to", twilioNumber, info, uuid from TwilioProxies where twilioNumber = $tn and "to" = $to"""
      .query[Proxy]
      .option

  def findByUUID(uuid: UUID) =
    sql"""select "from", "to", twilioNumber, info, uuid from TwilioProxies where uuid = $uuid"""
      .query[Proxy]
      .option

  def findByTwilio(tn: String) =
    sql"""select "from", "to", twilioNumber, info, uuid from TwilioProxies where twilioNumber = $tn"""
      .query[Proxy]
      .option
}
