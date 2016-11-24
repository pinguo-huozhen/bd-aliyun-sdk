package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}


class ExifRetrieve extends DataPlusUtil {

  def getExif(requestUrl: String) = {
    val httpclient: CloseableHttpClient = HttpClients.createDefault()
    val httpGet: HttpGet = new HttpGet(requestUrl)

    val response = httpclient.execute(httpGet)
    if (response.getStatusLine.getStatusCode == 200) {
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val inputStream = response.getEntity.getContent
      IOUtils.copy(inputStream, boStream)
      inputStream.close()
      httpclient.close()
      ImageResponse(200, new String(boStream.toByteArray))
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
