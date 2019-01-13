package implementations.doobs

import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor

import scalaz._
import Scalaz._
import scalaz.zio.IO
import scalaz.zio.interop._
import scalaz.zio.interop.catz._

import models.DatabaseInformation

trait TransactorProvider[F[_]] {
  def getTransactor: F[Transactor[F]]
}

final class DoobieTransactorProvider extends TransactorProvider[Task] {
  private var existingProviders: Map[String, Transactor[Task]] = Map.empty

  def getTransactor: Task[Transactor[Task]] =
    Task.point {
      Transactor.fromDriverManager[Task](
        "org.postgresql.Driver",
        s"jdbc:postgresql://localhost:5432/TwilioDemo",
        "Mark",
        ""
      )
    }
}
