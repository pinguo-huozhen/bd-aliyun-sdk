package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}


class ExifRetrieve extends DataPlusUtil with Serializable {

  def getExif(requestUrl: String, timeOut: Int = 5000) = {
    val httpclient: CloseableHttpClient = HttpClients.createDefault()
    val httpGet: HttpGet = new HttpGet(requestUrl)
    httpGet.setConfig(requestSetting(timeOut))

    val response = httpclient.execute(httpGet)
    if (response.getStatusLine.getStatusCode == SUCCESS_CODE) {
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpclient.close()
      ImageResponse(SUCCESS_CODE, new String(boStream.toByteArray))
    } else {
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpclient.close()
      ImageResponse(response.getStatusLine.getStatusCode, new String(boStream.toByteArray))
    }
  }
}

object ExifRetrieve {

}
