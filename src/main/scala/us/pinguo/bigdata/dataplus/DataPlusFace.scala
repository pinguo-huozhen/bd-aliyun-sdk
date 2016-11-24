package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import us.pinguo.bigdata.dataplus.DataPlusFace._

class DataPlusFace(signature: DataPlusSignature, organize_code: String) extends DataPlusUtil {
  implicit val formatter = DefaultFormats
  val requestURL = PATTERN_FACE_URL.format(organize_code)

  def faceDetect(body: Array[Byte]) = {
    val body = constructBody(0, 0, 1, signature.base64Encode(body))

    val headers = signature.header(requestURL, StringUtils.getBytesUtf8(body), HttpPost.METHOD_NAME)
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    val httpPost: HttpPost = new HttpPost(requestURL)
    headers.foreach(header => httpPost.setHeader(header._1, header._2))
    httpPost.setEntity(new StringEntity(body, "utf-8"))

    val response = httpClient.execute(httpPost)
    if (response.getStatusLine.getStatusCode == 200) {
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpClient.close()
      ImageResponse(200, new String(boStream.toByteArray))
    } else {
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpClient.close()
      ImageResponse(response.getStatusLine.getStatusCode, new String(boStream.toByteArray))
    }
  }

  private def constructBody(feaType: Int, landmarkType: Int, attrType: Int, base64Body: String) = {
    val settingMap = Map("image" -> DataSetting(50, base64Body),
      "fea_type" -> DataSetting(10, feaType),
      "landmark_type" -> DataSetting(10, landmarkType),
      "attr_type" -> DataSetting(10, attrType))
    Serialization.write(FaceBody(List(settingMap)))
  }


}

object DataPlusFace {
  val PATTERN_FACE_URL = "https://shujuapi.aliyun.com/%s/face/face_analysis"

  case class DataSetting(dataType: Int, dataValue: Any)

  case class FaceBody(inputs: List[Map[String, DataSetting]])

}