// package implementations.twilio

// import com.twilio.exception.ApiException
// import com.twilio.http.HttpMethod
// import io.chrisdavenport.log4cats.Logger
// import java.net.URI
// import java.time.Instant
// import scala.concurrent.duration._
// import scala.util.{ Failure, Success, Try }

// import simulacrum._
// import scalaz._
// import Scalaz._
// import scalaz.zio._
// import scalaz.zio.interop._
// import scalaz.zio.interop.scalaz72._

// import com.twilio.{ Twilio => TwilioSDK }
// import com.twilio.http.TwilioRestClient
// import com.twilio.`type`.{ PhoneNumber => TwilioSDKPhoneNumber }
// import com.twilio.rest.api.v2010.account.Message
// import com.twilio.converter.Promoter
// import com.twilio.rest.api.v2010.Account
// import scala.collection.JavaConverters._

// import interfaces._
// import models._
// import utilities._

// object SubAccounts {

//   private val rootAccId    = ""
//   private val rootAccToken = ""

//   def getExistingSubAccount: Task[Option[SubAccount]] =
//     for {
//       client <- getClientForRoot
//       acc <- IO.point {
//               println(s"Getting account")

//               val accs =
//                 Account.reader().setFriendlyName("testAccount").read().asScala
//               var acc: Account = null
//               for (record <- accs) {
//                 println(
//                   s"Account: ${record.getFriendlyName()}, sid: ${record.getSid()}"
//                 )
//                 if (acc == null) acc = record
//               }
//               Option(acc).map(
//                 a => SubAccount(acc.getSid(), acc.getAuthToken())
//               )
//             }
//     } yield acc

//   def createNewSubAccount: Task[SubAccount] =
//     for {
//       client <- getClientForRoot
//       acc <- IO.point {
//               val acc = Account.creator().setFriendlyName("testAccount").create()
//               println(
//                 s"Creating new account: ${acc.getFriendlyName()}, sid: ${acc.getSid()}"
//               )
//               SubAccount(acc.getSid(), acc.getAuthToken())
//             }

//     } yield acc

//   private def getClientForRoot: Task[TwilioRestClient] =
//     IO.point(new TwilioRestClient.Builder(rootAccId, rootAccToken).build())
// }
