package us.pinguo.bigdata.dataplus

import org.apache.http.client.config.RequestConfig


trait DataPlusUtil extends Serializable {

  case class ImageResponse(code: Int, json: String)

  val SUCCESS_CODE = 200

  val DEFAULT_TIMEOUT = 5000

  def requestSetting(timeOut: Int = 5000) = {
    RequestConfig.custom()
      .setConnectTimeout(timeOut)
      .setConnectionRequestTimeout(timeOut)
      .setSocketTimeout(timeOut)
      .build()
  }

}
