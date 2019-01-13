package interfaces.kafka

import scalaz._
import Scalaz._
import fs2._

trait Broker[F[_], A] {
  def consume: Stream[F, A]
}
