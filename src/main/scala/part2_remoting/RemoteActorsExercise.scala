package part2_remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, ActorSystem, Identify, PoisonPill, Props}
import com.typesafe.config.ConfigFactory

object WordCountDomain {
  case class Initialize(nWorkers: Int)
  case class WordCountTask(text: String)
  case class WordCountResult(count: Int)
  case object WordCount
}

class WordCountWorker extends Actor with ActorLogging {
  import WordCountDomain._
  override def receive: Receive = {
    case WordCountTask(text) =>
      log.info(s"I am processing $text")
      sender() ! WordCountResult(text.split(" ").length)
  }
}
class WordCountMaster extends Actor with ActorLogging{
  import WordCountDomain._
  val workers :List[ActorRef] = List()
  override def receive: Receive = {
    case Initialize(nWorkers) =>
      log.info("Master initializing")
      val selection =  (1 to nWorkers).map(i => context.actorSelection(s"akka://RemoteSystem@localhost:2552/user/wordCountWorker$i"))
      selection.foreach(_ ! Identify("gbm"))
      context.become(initializing(List(),nWorkers))
  }
  def initializing(workers: List[ActorRef], remainingWorkers:Int): Receive ={
    case ActorIdentity("gbm",Some(workerRef)) =>
      log.info(s"Worker identified: $workerRef")
      if(remainingWorkers == 1) context.become(online(workerRef :: workers, 0,0))
      else context.become(initializing(workerRef :: workers, remainingWorkers - 1))
  }
  def online(workers:List[ActorRef], remainingTasks: Int, totalCount: Int): Receive ={
    case text: String =>
      val sentences = text.split("\\. ")
      Iterator.continually(workers).flatten.zip(sentences.iterator).foreach{pair =>
        val (worker, sentence) = pair
        worker ! WordCountTask(sentence)
      }
      context.become(online(workers, remainingTasks + sentences.length, totalCount))
    case WordCountResult(count) =>
      if(remainingTasks==1){
        log.info(s"TOTAL RESULT ${totalCount + count}")
        workers.foreach(_ ! PoisonPill)
        context.stop(self)
      }
      else{
        context.become(online(workers, remainingTasks - 1, totalCount + count))
      }
  }
}
object MasterApp extends App{
  import WordCountDomain._

  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2551
    """.stripMargin)
    .withFallback(ConfigFactory.load("part2_remoting/remoteActorsExercise.conf"))

  val system = ActorSystem("MasterSystem", config)

  val master = system.actorOf(Props[WordCountMaster], "wordCountMaster")

  master ! Initialize(5)
  Thread.sleep(1000)
  scala.io.Source.fromFile("src/main/resources/txt/lipsum.txt").getLines().foreach { line =>
    master ! line
  }
}
object WorkerApp extends App{
  import WordCountDomain._
  val config = ConfigFactory.parseString(
    """
      |akka.remote.artery.canonical.port = 2552
    """.stripMargin)
    .withFallback(ConfigFactory.load("part2_remoting/remoteActorsExercise.conf"))

  val system = ActorSystem("WorkersSystem", config)

  (1 to 5).map(i => system.actorOf(Props[WordCountWorker],s"wordCountWorker$i"))
}
