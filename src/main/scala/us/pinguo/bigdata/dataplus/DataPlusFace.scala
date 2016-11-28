package us.pinguo.bigdata.dataplus

import java.util.regex.Pattern

import org.apache.commons.codec.binary.StringUtils
import org.apache.http.client.methods.HttpPost
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import org.json4s.jackson.Serialization
import us.pinguo.bigdata.api.PhotoTaggingAPI.{FaceResponse, FaceTag, PhotoTaggingException}
import us.pinguo.bigdata.dataplus.DataPlusFace._
import us.pinguo.bigdata.http

class DataPlusFace(signature: DataPlusSignature, organize_code: String) extends DataPlusUtil {

  private val requestURL = PATTERN_FACE_URL.format(organize_code)

  def request(body: Array[Byte], timeOut: Int = DEFAULT_TIMEOUT): FaceTag = {
    val base64Body = constructBody(0, 0, 1, signature.base64Encode(body))
    val headers = signature.header(requestURL, StringUtils.getBytesUtf8(base64Body), HttpPost.METHOD_NAME)
    val json = http(requestURL)
      .headers(headers.toArray: _*)
      .postString(base64Body)
      .requestForString
    parseResponse(json)
  }

  private def constructBody(feaType: Int, landmarkType: Int, attrType: Int, base64Body: String) = {
    implicit val formatter = DefaultFormats
    val settingMap = Map("image" -> DataSetting(50, base64Body),
      "fea_type" -> DataSetting(10, feaType),
      "landmark_type" -> DataSetting(10, landmarkType),
      "attr_type" -> DataSetting(10, attrType))
    Serialization.write(FaceBody(List(settingMap)))
  }

  private def parseResponse(body: String) = {
    implicit val formatter = DefaultFormats
    val json = parse(body)
    var jsonString = compact(render((json \ "outputs") (0) \ "outputValue" \ "dataValue"))
    jsonString = jsonString.substring(1, jsonString.indexOf("\\n")).replaceAll(Pattern.quote("\\"), "")

    val faceResponse = parse(jsonString).extract[FaceResponse]
    if (faceResponse.errno == 0) {
      FaceTag(faceResponse.age, faceResponse.gender, faceResponse.landmark, faceResponse.number, faceResponse.rect.sliding(4, 4).toList)
    } else throw PhotoTaggingException(faceResponse.errno, s"face response errno [${faceResponse.errno}]")
  }
}

object DataPlusFace {

  val PATTERN_FACE_URL = "https://shujuapi.aliyun.com/%s/face/face_analysis_aliyun" //https://shujuapi.aliyun.com/dataplus_62655/face/face_analysis_aliyun

  case class DataSetting(dataType: Int, dataValue: Any)

  case class FaceBody(inputs: List[Map[String, DataSetting]])

}