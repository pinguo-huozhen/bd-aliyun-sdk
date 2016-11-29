package us.pinguo.bigdata

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}


class PhotoTaggingTask(etag: String) extends Actor with ActorLogging {

  var results = Map[String, ]

  override def receive: Receive = {

  }
}
