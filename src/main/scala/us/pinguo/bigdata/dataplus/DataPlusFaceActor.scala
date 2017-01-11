package us.pinguo.bigdata.dataplus

import java.util.regex.Pattern

import akka.actor.Props
import org.apache.commons.codec.binary.StringUtils
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import org.json4s.jackson.Serialization._
import us.pinguo.bigdata.DataPlusActor.{FaceResponse, FaceTag, TaggingError}
import us.pinguo.bigdata.PhotoStatisticActor.Report
import us.pinguo.bigdata.dataplus.DataPlusFaceActor._
import us.pinguo.bigdata.{DataPlusActor, HttpStatusCodeError, PhotoStatisticActor, http}
import akka.pattern._
import com.ning.http.client.Response

import scala.concurrent.duration._
import scala.language.postfixOps

class DataPlusFaceActor(signature: DataPlusSignature, organize_code: String) extends DataPlusActor {
  import context._
  private val requestURL = PATTERN_FACE_URL.format(organize_code)
  val report = context.actorSelection("/user/report")
  private var processingBody:Array[Byte] = _

  override def receive: Receive = {
    case RequestFace(body) =>
      processingBody = body
      val base64Body = constructBody(1, 0, 1, signature.base64Encode(processingBody))
      val headers = signature.header(requestURL, StringUtils.getBytesUtf8(base64Body), "POST")
      pipe(http(requestURL)
        .headers(headers.toArray: _*)
        .postString(base64Body)
        .request) to self

      report ! Report("dp-face", PhotoStatisticActor.REQUEST)

    case Left(e:HttpStatusCodeError) =>
      report ! Report("dp-face", e.getCode.toString)
      parent ! TaggingError(s"DataPlusFaceActor-> http error:[${e.getMessage}]")

    case Right(response: Response) =>
      response.getStatusCode match {
        case SERVER_BUSY =>
          context.system.scheduler.scheduleOnce(DEFAULT_MILLS millis, self, RequestFace(processingBody))
          report ! Report("dp-face", PhotoStatisticActor.RETRY)

        case SUCCESS_CODE => parent ! parseResponse(response.getResponseBody)

        case _ => parent ! TaggingError(s"DataPlusFaceActor->code:[${response.getStatusCode}] body[${response.getResponseBody.replaceAll("\n","")}]")
      }

  }

  private def constructBody(feaType: Int, landmarkType: Int, attrType: Int, base64Body: String) = {
    implicit val formatter = DefaultFormats
    val settingMap = Map("image" -> DataSetting(50, base64Body),
      "fea_type" -> DataSetting(10, feaType),
      "landmark_type" -> DataSetting(10, landmarkType),
      "attr_type" -> DataSetting(10, attrType))
    write(FaceBody(List(settingMap)))
  }

  private def parseResponse(body: String) = {
    try {
      implicit val formatter = DefaultFormats
      val json = parse(body)
      var jsonString = compact(render((json \ "outputs") (0) \ "outputValue" \ "dataValue"))
      jsonString = jsonString.substring(1, jsonString.indexOf("\\n")).replaceAll(Pattern.quote("\\"), "")

      val faceResponse = parse(jsonString).extract[FaceResponse]
      if (faceResponse.errno == 0) {
        FaceTag(faceResponse.age, faceResponse.gender, faceResponse.landmark, faceResponse.number, faceResponse.rect.sliding(4, 4).toList)
      } else TaggingError(s"DataPlusFaceActor-> error:[${faceResponse.errno} body:[${jsonString.replaceAll("\n","")}]")
    } catch {
      case ex: Exception => TaggingError(s"DataPlusFaceActor-> parse error:[${ex.getMessage}]")
    }
  }

}

object DataPlusFaceActor {

  val PATTERN_FACE_URL = "https://shujuapi.aliyun.com/%s/face/face_analysis_aliyun"



  def props(signature: DataPlusSignature, organize_code: String) = Props(new DataPlusFaceActor(signature, organize_code))

  case class DataSetting(dataType: Int, dataValue: Any)

  case class FaceBody(inputs: List[Map[String, DataSetting]])

  case class RequestFace(body: Array[Byte])

}