package tests.utilities

import implementations.doobs.TransactorProvider
import models.DatabaseInformation
import scalaz._
import Scalaz._

import scalaz.zio._
import scalaz.zio.IO
import scalaz.zio.interop._
import scalaz.zio.interop.Task
import scalaz.zio.interop.scalaz72._
import scalaz.zio.interop.catz._

import doobie._
import doobie.h2._
import doobie.h2.implicits._
import doobie.implicits._

import java.util.UUID
import doobie.Types
import doobie.h2.`package`.H2Transactor

object TestDatabase {

  //every test gets its own db, if we end up with memory issues during testing look here
  def inMemoryTransactor: Task[H2Transactor[Task]] =
    H2Transactor.newH2Transactor[Task](
      s"jdbc:h2:mem:${UUID.randomUUID};DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "ioffice",
      ""
    )

  val createPendingProxiesTable: Types#Update0 = sql"""
    CREATE TABLE IF NOT EXISTS TwilioProxies (
        uuid uuid DEFAULT random_uuid() PRIMARY KEY NOT NULL,
        "from" VARCHAR(255) NOT NULL,
        "to" VARCHAR(255) NOT NULL,
        twilioNumber VARCHAR(255) UNIQUE,
        info VARCHAR(255),
        dateCreated TIMESTAMP DEFAULT current_timestamp() NOT NULL
    );
  """.update

  val createTwilioPhoneNumbersTable = sql"""
    CREATE TABLE IF NOT EXISTS TwilioPhoneNumbers (
        uuid uuid DEFAULT random_uuid() PRIMARY KEY NOT NULL,
        sid VARCHAR(255) NOT NULL UNIQUE,
        number VARCHAR(255) NOT NULL UNIQUE,
        dateCreated TIMESTAMP DEFAULT current_timestamp() NOT NULL,
        inUse BOOLEAN NOT NULL,
        willBeAvailable TIMESTAMP
    );
  """.update

  def createTables(transactor: Transactor[Task]): Task[Unit] = {
    val createConnection = for {
      _ <- createPendingProxiesTable.run
      _ <- createTwilioPhoneNumbersTable.run
    } yield ()
    createConnection.transact(transactor)
  }

  def resetEverything(transactor: Transactor[Task]): Task[Unit] =
    (for {
      _ <- sql"DROP ALL OBJECTS".update.run
      _ <- createPendingProxiesTable.run
      _ <- createTwilioPhoneNumbersTable.run
    } yield ()).transact(transactor).map(_ => ())
}

trait InMemoryDB extends RTS {
  import TestDatabase._
  val transactor: Task[H2Transactor[Task]] = inMemoryTransactor
  final class TestTransactorProvider extends TransactorProvider[Task] {
    def getTransactor: Task[Transactor[Task]] =
      transactor
  }
  val tp: InMemoryDB#TestTransactorProvider = new TestTransactorProvider()

  unsafeRun(transactor.flatMap(t => createTables(t)))
  val rollBackTest: () => Unit = () =>
    unsafeRun(transactor.flatMap(t => resetEverything(t)))
}
