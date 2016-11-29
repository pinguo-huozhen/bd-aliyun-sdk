package us.pinguo.bigdata.dataplus

import akka.actor.Props
import org.json4s.DefaultFormats
import us.pinguo.bigdata.{DataPlusActor, http}
import us.pinguo.bigdata.dataplus.ExifRetrieveActor.RequestExif

import scala.concurrent.duration._
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.DataPlusActor.{ExifTag, TaggingError}

import scala.language.postfixOps


class ExifRetrieveActor extends DataPlusActor {

  import context._

  implicit val formatter = DefaultFormats

  override def receive: Receive = {
    case RequestExif(requestUrl) =>
      val result = http(requestUrl)
        .request

      result.map {
        case Left(e) => parent ! TaggingError(e.getMessage)
        case Right(response) =>
          if (response.getStatusCode == SERVER_BUSY) context.system.scheduler.scheduleOnce(DEFAULT_MILLS millis, self, RequestExif(requestUrl))
          else if (response.getStatusCode == SUCCESS_CODE) {
            try {
              parent ! read[ExifTag](response.getResponseBody)
            } catch {
              case ex: Exception => TaggingError(ex.getMessage)
            }
          }
          else parent ! TaggingError(response.getResponseBody)
      }
  }
}

object ExifRetrieveActor {

  case class RequestExif(requestUrl: String)

  def props() = Props(new ExifRetrieveActor())

}
