package us.pinguo.bigdata

import akka.actor.{Actor, ActorLogging, Props}
import us.pinguo.bigdata.PhotoStatisticActor.{Report, Stat}

import scala.collection.mutable


class PhotoStatisticActor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("PhotoStatisticActor started")

  val queryStat = mutable.Map.empty[String, Map[String, Long]]

  override def receive: Receive = {
    case Report(name, queryType) =>
      queryStat.get(name) match {
        case Some(map) =>
          map.get(queryType) match {
            case Some(x:Long) => queryStat(name) += (queryType -> x.+(1))
            case None => queryStat(name) += (queryType -> 1)
          }
        case None => queryStat += (name -> Map(queryType -> 1))
      }

    case Stat => sender() ! queryStat

  }
}

object PhotoStatisticActor {

  def props = Props(new PhotoStatisticActor)

  val REQUEST = "request"
  val RETRY = "retry"

  case class Report(name: String, queryType: String)

  case object Stat

}
