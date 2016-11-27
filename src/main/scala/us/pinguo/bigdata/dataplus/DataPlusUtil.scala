package us.pinguo.bigdata.dataplus

import org.apache.http.client.config.RequestConfig
import us.pinguo.bigdata.dataplus.DataPlusUtil.ImageResponse


trait DataPlusUtil extends Serializable {

  val SUCCESS_CODE = 200

  val SERVER_BUSY = 503

  val FATAL_CODE = 400

  val DEFAULT_TIMEOUT = 5000

  val DEFAULT_RETRY = 3

  def requestSetting(timeOut: Int = DEFAULT_TIMEOUT): RequestConfig = {
    RequestConfig.custom()
      .setConnectTimeout(timeOut)
      .setConnectionRequestTimeout(timeOut)
      .setSocketTimeout(timeOut)
      .build()
  }

  def retry(name: String, f: => ImageResponse, times: Int = DEFAULT_RETRY): ImageResponse = {
    val start = System.currentTimeMillis()
    var count = times
    var response: ImageResponse = null
    do {
      response = f
      count -= 1
      if (response.code == SERVER_BUSY) Thread.sleep(500)
    } while (response.code == SERVER_BUSY && count > 0)
    val cost = System.currentTimeMillis() - start
    println(s"request [name] cost [$cost]")
    response
  }

}

object DataPlusUtil {

  case class ImageResponse(code: Int, json: String)

}
