package us.pinguo.bigdata.dataplus

import java.io.ByteArrayOutputStream

import akka.actor.Actor.Receive
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import us.pinguo.bigdata.{DataPlusActor, http}
import us.pinguo.bigdata.dataplus.DataPlusUtil.ImageResponse
import us.pinguo.bigdata.dataplus.ExifRetrieve.RequestExif


class ExifRetrieve extends DataPlusActor {

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

  override def receive: Receive = {
    case RequestExif(requestUrl) =>
      val result = http(requestUrl)
        .request

      result.map {
        case Left(e) =>
        case Right(response) =>
      }
  }
}

object ExifRetrieve {

  case class RequestExif(requestUrl: String)

  case class ExifError(code: Int, message: String)

}
