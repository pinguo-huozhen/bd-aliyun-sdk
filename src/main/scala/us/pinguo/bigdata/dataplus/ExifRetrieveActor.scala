package us.pinguo.bigdata.dataplus

import akka.actor.Props
import org.json4s.DefaultFormats
import us.pinguo.bigdata.{DataPlusActor, http}
import us.pinguo.bigdata.dataplus.ExifRetrieveActor.RequestExif
import akka.pattern._
import com.ning.http.client.Response

import scala.concurrent.duration._
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.DataPlusActor.{ExifTag, TaggingError}

import scala.language.postfixOps


class ExifRetrieveActor extends DataPlusActor {

  import context._

  implicit val formatter = DefaultFormats
  private var processingUrl: String = _

  override def receive: Receive = {
    case RequestExif(imageUrl) =>
      processingUrl = imageUrl
      val requestUrl = if(imageUrl.contains("?")) s"$imageUrl&exif" else s"$imageUrl?exif"
      pipe(http(requestUrl).request) to self

    case Left(e: Exception) => parent ! TaggingError(s"ExifRetrieveActor-> http error:[${e.getMessage}]")

    case Right(response: Response) =>
      response.getStatusCode match {
        case SERVER_BUSY => context.system.scheduler.scheduleOnce(DEFAULT_MILLS millis, self, RequestExif(processingUrl))

        case SUCCESS_CODE =>
          try parent ! read[ExifTag](response.getResponseBody) catch {
            case ex: Exception => TaggingError(s"ExifRetrieveActor-> parse error:[${ex.getMessage}]")
          }

        case _ =>
          parent ! TaggingError(s"ExifRetrieveActor->code:[${response.getStatusCode}] body[${response.getResponseBody.replaceAll("\n","")}]")
      }

  }
}

object ExifRetrieveActor {

  case class RequestExif(requestUrl: String)

  def props() = Props(new ExifRetrieveActor())

}
