package part3_clustering

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import com.typesafe.config.ConfigFactory
import part3_clustering.ChatDomain.{EnterRoom, UserMessage}

object ChatDomain {
  case class ChatMessage(nickname: String, contents: String)
  case class UserMessage(contents: String)
  case class EnterRoom(fullAdress: String, nickname: String)
}

class ChatActor(nickname: String, port: Int) extends Actor with ActorLogging {
  import ChatDomain._
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }
  override def receive: Receive = ???

  def online(chatRoom: Map[String, String]):Receive ={
    case MemberUp(member) =>
      val actorSelection = context.actorSelection(s"${member.address}/user/chatActor")
      actorSelection ! EnterRoom(s"${self.path.address}@localhost:$port", nickname)
    case MemberRemoved(member, _) =>
      context.become(online(chatRoom - member.address.toString))
    case EnterRoom(remoteAddress, nickname) =>
      context.become(online(chatRoom + (remoteAddress -> nickname)))
    case UserMessage(contents) =>
      chatRoom.foreach(cr =>{
        val actorRef = context.actorSelection(cr._1+"/user/chatActor")
        actorRef ! ChatMessage(nickname, contents)
      })

  }
}
class ChatApp(nickname: String, port: Int) extends App{
  import ChatDomain._

  val config = ConfigFactory.parseString(
    s"""
       |akka.remote.artery.canonical.port = $port
     """.stripMargin)
    .withFallback(ConfigFactory.load("part3_clustering/clusterChat.conf"))

  val system = ActorSystem("RTJVMCluster", config)
  val chatActor = system.actorOf(Props[ChatActor],"chatActor")
  scala.io.Source.stdin.getLines().foreach{line =>
    chatActor ! UserMessage(line)
  }
}
object Alice extends ChatApp("Alice",2551)
object Bob extends ChatApp("Bob",2552)
object Charlie extends ChatApp("Charlie",2553)