package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import us.pinguo.bigdata.dataplus.DataPlusUtil.ImageResponse


class ExifRetrieve extends DataPlusUtil with Serializable {

  def request(requestUrl: String, timeOut: Int = DEFAULT_TIMEOUT) = {
    try {
      val httpclient: CloseableHttpClient = HttpClients.createDefault()
      val httpGet: HttpGet = new HttpGet(requestUrl)
      httpGet.setConfig(requestSetting(timeOut))

      val response = httpclient.execute(httpGet)
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpclient.close()
      ImageResponse(response.getStatusLine.getStatusCode, new String(boStream.toByteArray))
    } catch {
      case ex: Exception => ImageResponse(FATAL_CODE, ex.getMessage)
    }
  }

}
