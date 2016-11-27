package us.pinguo.bigdata.dataplus

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import us.pinguo.bigdata.api.PhotoTaggingAPI.ItemTag
import us.pinguo.bigdata.dataplus.DataPlusItem._
import us.pinguo.bigdata.http

class DataPlusItem(signature: DataPlusSignature, organize_code: String) extends DataPlusUtil {

  private val requestURL = PATTERN_ITEM_URL.format(organize_code)

  def request(body: Array[Byte], timeOut: Int = DEFAULT_TIMEOUT): ItemTag = {
    implicit val formatter = DefaultFormats
    val headers = signature.header(requestURL, body, "PUT", "*/*", "*/*")
    val json = http(requestURL)
      .headers(headers.toArray: _*)
      .putByte(body)
      .requestForString
    parse(json).extract[ItemTag]
  }
}

object DataPlusItem {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/upload?x-oss-process=udf/visual/detect"

}
