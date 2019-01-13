package logic

import io.chrisdavenport.log4cats.Logger
import scala.concurrent.duration._
import simulacrum._
import scalaz._
import Scalaz._

import utilities._
import interfaces._
import models._

final class Proxies[F[_]](
  twilio: Twilio[F],
  proxiesRepo: ProxiesRepository[F],
  phoneNumbers: PhoneNumbers[F],
  conf: ServerConfig
)(implicit F: Monad[F], L: Logger[F]) {
  def startProxy(
    inbound: InboundTwilioText
  ): F[Option[ProxyInfo]] =
    Logger[F].info(
      s"About to try and start a proxy for inbound text $inbound"
    ) *>
      (for {
        maybeProxy <- OptionT(proxiesRepo.find(inbound.from, inbound.to))
        newProxyInfo <- OptionT(
                         twilio.startProxy(maybeProxy, inbound.body)
                       )
        updatedProxy <- proxiesRepo
                         .update(
                           maybeProxy
                             .copy(info = Some(newProxyInfo))
                         )
                         .liftM[OptionT]
        _ <- twilio
              .scheduleProxyEnd(
                updatedProxy,
                conf.proxyDuration,
                (pp: Proxy) => deleteProxy(pp) *> freeNumber(pp)
              )
              .liftM[OptionT]
      } yield newProxyInfo).run

  def createProxy(
    from: ValidatedPhoneNumber,
    to: ValidatedPhoneNumber,
    twilioNum: TwilioNumber
  ): F[Unit] =
    for {
      _ <- Logger[F].info(
            s"Sending new Proxy to the db"
          )
      pp = Proxy(from.num, to.num, twilioNum.num, None, None)
      _ <- proxiesRepo
            .save(pp)
            .map(_ => ())
      _ <- Logger[F].info(
            s"scheduling the deletion of the pending proxy"
          )
      _ <- scheduleDeleteProxy(pp, conf.proxyDuration)
    } yield ()

  def scheduleDeleteProxy(
    pp: Proxy,
    d: Duration
  ): F[Unit] = proxiesRepo.scheduleDelete(pp, d)

  private def deleteProxy(pp: Proxy): F[Unit] =
    Logger[F].info(
      s"About to delete proxy with twilionumber ${pp.twilioNumber} from the db"
    ) *>
      proxiesRepo
        .delete(pp)
        .map(_ => ())

  private def freeNumber(pp: Proxy): F[Unit] =
    (for {
      _ <- Logger[F]
            .info(s"About to free up ${pp.twilioNumber}")
            .liftM[OptionT]
      tn <- OptionT(phoneNumbers.find(pp.twilioNumber))
      _  <- phoneNumbers.setAvailable(tn).liftM[OptionT]
    } yield ()).run.map(_.getOrElse(()))
}
