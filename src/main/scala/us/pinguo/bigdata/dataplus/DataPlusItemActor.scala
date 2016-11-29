package us.pinguo.bigdata.dataplus

import akka.actor.Props
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.DataPlusActor.{ItemTag, TaggingError}
import us.pinguo.bigdata.dataplus.DataPlusItemActor._
import us.pinguo.bigdata.{DataPlusActor, http}

import scala.concurrent.duration._

class DataPlusItemActor(signature: DataPlusSignature, organize_code: String) extends DataPlusActor {
  import context._
  private val requestURL = PATTERN_ITEM_URL.format(organize_code)

  override def receive: Receive = {
    case RequestItem(body) =>
      implicit val formatter = DefaultFormats
      val headers = signature.header(requestURL, body, "PUT", "*/*", "*/*")
      val result = http(requestURL)
        .headers(headers.toArray: _*)
        .putByte(body)
        .request

      result.map {
        case Left(e) => parent ! TaggingError(e.getMessage)
        case Right(response) =>
          if (response.getStatusCode == SERVER_BUSY) context.system.scheduler.scheduleOnce(DEFAULT_MILLS millis, self, RequestItem(body))
          else if (response.getStatusCode == SUCCESS_CODE) {
            try {
              parent ! read[ItemTag](response.getResponseBody)
            } catch {
              case ex: Exception => TaggingError(ex.getMessage)
            }
          }
          else parent ! TaggingError(response.getResponseBody)
      }
  }
}

object DataPlusItemActor {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/upload?x-oss-process=udf/visual/detect"

  case class RequestItem(body: Array[Byte])

  def props(signature: DataPlusSignature, organize_code: String) = Props(new DataPlusItemActor(signature, organize_code))

}
