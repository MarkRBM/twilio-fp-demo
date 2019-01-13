package implementations.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.concurrent.ExecutionContext
import fs2.Stream
import com.ovoenergy.fs2.kafka._
import org.apache.kafka.common.serialization._
import org.apache.kafka.streams._
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import org.apache.kafka.common.serialization._
import com.lightbend.kafka.scala.streams._
import interfaces.kafka.SmsStream
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._
import kafkatopics.models._
import utilities.ServerConfig

final class SmsNotificationsBroker(config: ServerConfig)(
  implicit ec: ExecutionContext
) extends SmsStream[Task] {

  private val keySerde: Serde[SmsNotificationsKeyAvro] =
    new SpecificAvroSerde[SmsNotificationsKeyAvro]
  private val valueSerde: Serde[SmsNotificationsValueAvro] =
    new SpecificAvroSerde[SmsNotificationsValueAvro]

  def consume
    : Stream[Task, (SmsNotificationsKeyAvro, SmsNotificationsValueAvro)] =
    consumeProcessAndCommit[Task](
      TopicSubscription(Set(config.kafkaConfig.topicName)),
      keySerde.deserializer(),
      valueSerde.deserializer(),
      config.kafkaConfig.getConsumerSettings
    )(processRecord)

  private def processRecord(
    r: ConsumerRecord[SmsNotificationsKeyAvro, SmsNotificationsValueAvro]
  ): Task[(SmsNotificationsKeyAvro, SmsNotificationsValueAvro)] = Task.point {
    (r.key(), r.value())
  }
}
