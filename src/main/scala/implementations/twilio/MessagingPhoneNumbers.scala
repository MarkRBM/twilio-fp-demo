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
import com.twilio.base.ResourceSet
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.Local
import com.twilio.`type`.{ PhoneNumber => AvaialbleTwilioPhoneNumber }
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber

import interfaces._
import models._
import utilities._

object MessagingPhoneNumbers {
  def create(
    conf: ServerConfig
  ): Task[TwilioNumber] =
    for {
      availableNumber <- findAvailableNumber
      savedNumber <- IO.point {
                      //the webhook url can / should be set here
                      IncomingPhoneNumber
                        .creator(availableNumber)
                        .setSmsMethod(HttpMethod.GET)
                        .setSmsUrl(
                          toCustomerCallbackUrl(conf.callbackurl)
                        )
                        .setSmsFallbackMethod(HttpMethod.GET)
                        .setSmsFallbackUrl(
                          toCustomerCallbackUrl(conf.callbackurl)
                        )
                        .create()
                    }
      sid                = savedNumber.getSid
      number             = savedNumber.getPhoneNumber.toString
      created            = Epoch.now
      toBeReturnedNumber = TwilioNumber(sid, number, false, None, None, None)
    } yield toBeReturnedNumber

  private def findAvailableNumber: Task[AvaialbleTwilioPhoneNumber] = IO.point {
    //need to probably pass in the customers location here and also
    // I dont know the implications of the setExcludelocaladdressrequired
    val numbers =
      Local.reader("US").setExcludeLocalAddressRequired(true).read()
    val number = numbers.iterator.next.getPhoneNumber
    number
  }

  def resetNumberSettingsAfterProxyEnds(
    twilioNumber: String,
    conf: ServerConfig
  )(implicit L: Logger[Task]): Task[Unit] =
    (for {
      sid <- OptionT(getSidByNumber(twilioNumber))
      _ <- Task.point {
            IncomingPhoneNumber
              .updater(sid)
              .setSmsMethod(HttpMethod.GET)
              .setSmsUrl(toCustomerCallbackUrl(conf.callbackurl))
              .setSmsFallbackMethod(HttpMethod.GET)
              .setSmsFallbackUrl(
                toCustomerCallbackUrl(conf.callbackurl)
              )
              .setVoiceMethod(HttpMethod.GET)
              .setVoiceUrl(toCustomerCallbackUrl(conf.callbackurl))
              .setVoiceFallbackMethod(HttpMethod.GET)
              .setVoiceFallbackUrl(
                toCustomerCallbackUrl(conf.callbackurl)
              )
              .update()

          }.liftM[OptionT]
    } yield ()).run.map(_.getOrElse(()))

  def getSidByNumber(
    twilioNumber: String
  )(implicit L: Logger[Task]): Task[Option[String]] =
    Logger[Task].info(s"Trying to get Sid for $twilioNumber") *> IO.point {
      import scala.collection.JavaConverters._
      val numList                     = IncomingPhoneNumber.reader().read().asScala
      var number: IncomingPhoneNumber = null
      for (num <- numList) {
        val checkingNumber = num.getPhoneNumber()
        if (checkingNumber.toString == twilioNumber) {
          number = num
        }
      }
      Try(number.getSid())
        .map(sid => {
          sid
        })
        .toOption
    }

  private def toCustomerCallbackUrl(url: String) =
    s"$url/text-replies"
}
