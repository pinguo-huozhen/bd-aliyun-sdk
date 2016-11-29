package us.pinguo.bigdata.dataplus

import akka.actor.Props
import us.pinguo.bigdata.{DataPlusActor, http}
import us.pinguo.bigdata.dataplus.ExifRetrieveActor.{ExifError, RequestExif}

import scala.concurrent.duration._
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.api.PhotoTaggingAPI.ExifTag


class ExifRetrieveActor extends DataPlusActor {

  import context._

  override def receive: Receive = {
    case RequestExif(requestUrl) =>
      val result = http(requestUrl)
        .request

      result.map {
        case Left(e) => parent ! ExifError(FATAL_CODE, e.getMessage)
        case Right(response) =>
          if (response.getStatusCode == SERVER_BUSY) context.system.scheduler.scheduleOnce(500 millis, self, RequestExif(requestUrl))
          else if (response.getStatusCode == SUCCESS_CODE) parent ! read[ExifTag](response.getResponseBody)
          else parent ! ExifError(response.getStatusCode, response.getResponseBody)
      }
  }
}

object ExifRetrieveActor {

  case class RequestExif(requestUrl: String)

  case class ExifError(code: Int, message: String)

  def props() = Props(new ExifRetrieveActor)

}
