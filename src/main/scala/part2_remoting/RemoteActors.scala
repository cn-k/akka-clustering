package part2_remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorSelection, ActorSystem, Identify, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RemoteActors extends App {
  //both actors going to work on same jvm
  // 2 different actor system running on the same JVM
  val localSytsem = ActorSystem("LocalSystem", ConfigFactory.load("part2_remoting/remoteActors.conf"))
  val localSimpleActor = localSytsem.actorOf(Props[SimpleActor], "localSimpleActor")
  localSimpleActor ! "hello, remote actor"

  //send message to REMOTE simple actor
  // Method 1: actor selection

  val remoteActorSelection = localSytsem.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
  remoteActorSelection ! "hello from the \"local\" JVM"

  // Method 2: resolve the actor selection to an actor ref

  import localSytsem.dispatcher

  implicit val timeout = Timeout(3 seconds)
  remoteActorSelection.resolveOne().onComplete {
    case Success(actorRef) =>
      actorRef ! "I have resolved you in a future"
    case Failure(exception) =>
      println(s"I failed to resolve the remote actor because $exception")
  }
  //Method 3: actor identification via messages

  class ActorResolver extends Actor with ActorLogging {

    override def preStart(): Unit = {
      val selection: ActorSelection = context.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
      selection ! Identify(42)
    }

    override def receive: Receive = {
      case ActorIdentity(42, Some(actorRef)) =>
        actorRef ! "Thank you for identifying yourself! "
    }
  }

  localSytsem.actorOf(Props[ActorResolver],"localActorResolver")
}

object RemoteActors_Remote extends App {
  val remoteSytsem = ActorSystem("RemoteSystem", ConfigFactory.load("part2_remoting/remoteActors.conf").getConfig
  ("remoteSystem"))
  val remoteSimpleActor = remoteSytsem.actorOf(Props[SimpleActor], "remoteSimpleActor")
  remoteSimpleActor ! "hello, remote actor"
}