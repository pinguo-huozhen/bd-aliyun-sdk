package us.pinguo.bigdata.dataplus

import org.apache.http.client.config.RequestConfig
import us.pinguo.bigdata.dataplus.DataPlusUtil.ImageResponse


trait DataPlusUtil extends Serializable {

  val SUCCESS_CODE = 200

  val SERVER_DOWN_CODE = 503

  val FATAL_CODE = 400

  val DEFAULT_TIMEOUT = 5000

  val DEFAULT_RETRY = 3

  def requestSetting(timeOut: Int = DEFAULT_TIMEOUT) = {
    RequestConfig.custom()
      .setConnectTimeout(timeOut)
      .setConnectionRequestTimeout(timeOut)
      .setSocketTimeout(timeOut)
      .build()
  }

  def retry(f: => ImageResponse, times: Int = DEFAULT_RETRY):ImageResponse = {
    var count = times
    var response:ImageResponse = null
    do {
      println(count)
      response = f
      count -= 1
      if(response.code!=SUCCESS_CODE) Thread.sleep(500)
    } while(response.code!=SUCCESS_CODE && count>0)
    response
  }

}

object DataPlusUtil {
  case class ImageResponse(code: Int, json: String)
}
