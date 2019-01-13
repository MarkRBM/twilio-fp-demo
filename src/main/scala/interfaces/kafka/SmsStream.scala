package interfaces.kafka

import scalaz._
import Scalaz._
import fs2._
import kafkatopics.models._

trait SmsStream[F[_]]
    extends Broker[F, (SmsNotificationsKeyAvro, SmsNotificationsValueAvro)]
