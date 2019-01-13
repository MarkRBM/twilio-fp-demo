package implementations.twilio

import com.twilio.exception.ApiException
import com.twilio.http.HttpMethod
import io.chrisdavenport.log4cats.Logger
import java.net.URI
import java.time.Instant
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import simulacrum._
import scalaz._
import Scalaz._
import scalaz.zio._
import scalaz.zio.interop._
import scalaz.zio.interop.scalaz72._

import com.twilio.{ Twilio => TwilioSDK }
import com.twilio.http.TwilioRestClient
import com.twilio.`type`.{ PhoneNumber => TwilioSDKPhoneNumber }
import com.twilio.rest.api.v2010.account.Message
import com.twilio.converter.Promoter
import com.twilio.rest.lookups.v1.{ PhoneNumber => TwilioLookupNumber }

import interfaces._
import models._
import utilities._

object NumberLookup {
  def look(num: String): Task[TwilioLookupNumber] = IO.point {
    TwilioLookupNumber
      .fetcher(
        new com.twilio.`type`.PhoneNumber(num)
      )
      .setType(Promoter.listOfOne("carrier"))
      .fetch()
  }
}
