// package logic

// import io.chrisdavenport.log4cats.Logger
// import simulacrum._
// import scalaz._
// import Scalaz._

// import utilities._
// import models._
// import interfaces._

// final class SubAccounts[F[_]](
//   twilio: Twilio[F]
// )(implicit F: Monad[F], L: Logger[F]) {
//   def getCustomerAccount(c: Customer): F[SubAccount] =
//     for {
//       _   <- Logger[F].info("Trying to get Sub Account for ${c.appCode} from db")
//       acc <- twilio.getOrCreateSubAccount(c)
//       _   <- Logger[F].info("Got account ${acc.sid} from Twilio")
//     } yield acc
// }
