package models

import java.util.UUID
import utilities._

final case class DatabaseInformation(
  appCode: String,
  user: String,
  password: String,
  address: String,
  name: String
)
final case class TextMessageRequest(
  textMessage: SMS
)
sealed trait PhoneNumber {
  def num: String
}
object PhoneNumber {
  def apply(ph: String): UnvalidatedPhoneNumber = UnvalidatedPhoneNumber(ph)
}
final case class UnvalidatedPhoneNumber(num: String) extends PhoneNumber
final case class ValidatedPhoneNumber(num: String)   extends PhoneNumber

sealed trait TextMessage {
  def from: Option[PhoneNumber]
  def to: PhoneNumber
  def body: String
}
sealed trait ValidatedTextMessage extends TextMessage {
  def from: Option[ValidatedPhoneNumber]
  def to: ValidatedPhoneNumber
}
final case class SMS(
  from: Option[UnvalidatedPhoneNumber],
  to: UnvalidatedPhoneNumber,
  body: String
) extends TextMessage
// final case class MMS(from: PhoneNumber, to: PhoneNumber) extends TextMessage
final case class ValidatedSMS(
  from: Option[ValidatedPhoneNumber],
  to: ValidatedPhoneNumber,
  body: String
) extends ValidatedTextMessage
final case class ValidatedMMS(
  from: Option[ValidatedPhoneNumber],
  to: ValidatedPhoneNumber,
  body: String
) extends ValidatedTextMessage

sealed trait TwilioTextMessage {
  def from: Option[ValidatedPhoneNumber]
  def to: ValidatedPhoneNumber
  def body: String
}
final case class TwilioSMS(
  from: Option[ValidatedPhoneNumber],
  to: ValidatedPhoneNumber,
  body: String
) extends TwilioTextMessage
final case class TwilioMMS(
  from: Option[ValidatedPhoneNumber],
  to: ValidatedPhoneNumber,
  body: String,
  mediaUrl: String
) extends TwilioTextMessage

final case class TwilioNumber(
  sid: String,
  num: String,
  inUse: Boolean,
  created: Option[Epoch],
  willBeAvailable: Option[Epoch],
  uuid: Option[UUID]
)

final case class InboundTwilioText(
  messageSid: String,
  from: String,
  to: String,
  body: String
)

final case class ProxyInfo(id: String)
final case class Proxy(
  from: String,
  to: String,
  twilioNumber: String,
  info: Option[ProxyInfo],
  uuid: Option[UUID]
)
final case class AppInfoResponse(
  status: String,
  response: DatabaseInformation
)
