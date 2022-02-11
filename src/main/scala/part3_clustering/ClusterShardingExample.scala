package part3_clustering

import akka.actor.{Actor, ActorLogging, ActorRef}

import java.util.Date

case class OysterCard(id: String, amount: Double)
case class EntryAttempt(oysterCard:OysterCard, date: Date)
case object EntryAccepted
case class EntryRejected(reason: String)

class Turnstile(validator: ActorRef) extends Actor with ActorLogging{
  override def receive: Receive = {
    case o:OysterCard => validator ! EntryAttempt(o, new Date)
    case EntryAccepted => log.info("GREEN: please pass")
    case EntryRejected(reason) => log.info(s"RED $reason")
  }
}
class OysterCardValidator extends Actor with ActorLogging{
  override def preStart(): Unit = {
    super.preStart()
    log.info("Validator starting")
  }

  override def receive: Receive = {
    case EntryAttempt(card @ OysterCard(id,amount), _) =>
      log.info(s"validateing card with $id")
      if(amount>2.5)sender() ! EntryAccepted
      else sender() ! EntryRejected(s"[$id] not enought funds, please top up")
  }

}