package us.pinguo.bigdata.dataplus

import akka.actor.Props
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.DataPlusActor.{ItemTag, TaggingError}
import us.pinguo.bigdata.PhotoStatisticActor.Report
import us.pinguo.bigdata.dataplus.DataPlusItemActor._
import us.pinguo.bigdata.{DataPlusActor, HttpStatusCodeError, PhotoStatisticActor, http}
import akka.pattern._
import com.ning.http.client.Response

import scala.concurrent.duration._
import scala.language.postfixOps

class DataPlusItemActor(signature: DataPlusSignature, organize_code: String) extends DataPlusActor {

  import context._

  private val report = context.actorSelection("/user/report")

  private implicit val formatter = DefaultFormats

  private var processingBody: Array[Byte] = _


  override def receive: Receive = {
    case RequestItem(body) =>
      processingBody = body

      val requestURL = PATTERN_ITEM_URL.format(organize_code, signature.md5(body))
      val headers = signature.header(requestURL, body, "PUT", "*/*", "*/*")
      pipe(http(requestURL)
        .headers(headers.toArray: _*)
        .putByte(processingBody)
        .request) to self

      report ! Report("dp-item", PhotoStatisticActor.REQUEST)

    case Left(e: HttpStatusCodeError) =>
      parent ! TaggingError(s"DataPlusItemActor-> http error:[${e.getMessage}]")
      report ! Report("dp-item", e.getCode.toString)

    case Right(response: Response) =>
      response.getStatusCode match {

        case SERVER_BUSY =>
          context.system.scheduler.scheduleOnce(DEFAULT_MILLS millis, self, RequestItem(processingBody))
          report ! Report("dp-item", PhotoStatisticActor.RETRY)

        case SUCCESS_CODE =>
          try parent ! read[ItemTag](response.getResponseBody) catch {
            case ex: Exception => TaggingError(s"DataPlusItemActor-> parse error:[${ex.getMessage}]")
          }

        case _ =>
          parent ! TaggingError(s"DataPlusItemActor->code:[${response.getStatusCode}] processingBody[${response.getResponseBody.replaceAll("\n", "")}]")
      }
  }
}

object DataPlusItemActor {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/%s?x-oss-process=udf/visual/detect"

  case class RequestItem(body: Array[Byte])

  def props(signature: DataPlusSignature, organize_code: String) = Props(new DataPlusItemActor(signature, organize_code))

}
