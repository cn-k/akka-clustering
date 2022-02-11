package part2_remoting

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{ConfigBeanFactory, ConfigFactory}

object DeployingActorsRemotely_LocalApp extends App {
  val system = ActorSystem("LocalActorSystem", ConfigFactory.load("part2_remoting/deployingActorsRemotely.conf")
    .getConfig("localApp"))
  val simpleActor = system.actorOf(Props[SimpleActor],"remoteActor")
  simpleActor ! "hello remote actor"
}
object DeployingActorsRemotely_RemoteApp extends App {
  val system = ActorSystem("RemoteActorSystem", ConfigFactory.load("part2_remoting/deployingActorsRemotely.conf")
    .getConfig("remoteApp"))
}
