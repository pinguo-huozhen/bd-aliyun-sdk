package us.pinguo.bigdata.dataplus

import akka.actor.Actor.Receive
import dispatch.Future
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.api.PhotoTaggingAPI.{ExifTag, ItemTag}
import us.pinguo.bigdata.dataplus.DataPlusItem._
import us.pinguo.bigdata.{DataPlusActor, http}
import scala.concurrent.duration._

class DataPlusItem(signature: DataPlusSignature, organize_code: String) extends DataPlusActor {

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
        case Left(e) => sender() ! ItemError(FATAL_CODE, e.getMessage)
        case Right(response) =>
          if (response.getStatusCode == SERVER_BUSY) context.system.scheduler.scheduleOnce(500 millis, self, RequestItem(body))
          else if (response.getStatusCode == SUCCESS_CODE) sender() ! read[ItemTag](response.getResponseBody)
          else sender() ! ItemError(response.getStatusCode, response.getResponseBody)
      }
  }
}

object DataPlusItem {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/upload?x-oss-process=udf/visual/detect"

  case class RequestItem(body: Array[Byte])

  case class ItemError(code: Int, messge: String)

}
