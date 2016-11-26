package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import us.pinguo.bigdata.dataplus.DataPlusItem._
import us.pinguo.bigdata.dataplus.DataPlusUtil.ImageResponse

class DataPlusItem(signature: DataPlusSignature, organize_code: String) extends DataPlusUtil {

  val requestURL = PATTERN_ITEM_URL.format(organize_code)

  def request(body: Array[Byte], timeOut: Int = DEFAULT_TIMEOUT) = {
    try {
      val headers = signature.header(requestURL, body, HttpPut.METHOD_NAME, "*/*", "*/*")

      val httpclient: CloseableHttpClient = HttpClients.createDefault()
      val httpPut: HttpPut = new HttpPut(requestURL)
      httpPut.setConfig(requestSetting(timeOut))
      headers.foreach(header => httpPut.addHeader(header._1, header._2))
      httpPut.setEntity(new ByteArrayEntity(body))

      val response = httpclient.execute(httpPut)
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpclient.close()
      ImageResponse(response.getStatusLine.getStatusCode, new String(boStream.toByteArray))
    } catch {
      case ex:Exception => ImageResponse(FATAL_CODE, ex.getMessage)
    }
  }

}

object DataPlusItem {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/upload?x-oss-process=udf/visual/detect"

}
