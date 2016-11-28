package us.pinguo.bigdata.dataplus

import dispatch.Future
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import us.pinguo.bigdata.api.PhotoTaggingAPI.{ExifTag, ItemTag}
import us.pinguo.bigdata.dataplus.DataPlusItem._
import us.pinguo.bigdata.http

class DataPlusItem(signature: DataPlusSignature, organize_code: String) extends DataPlusUtil {

  private val requestURL = PATTERN_ITEM_URL.format(organize_code)

  def request(body: Array[Byte], timeOut: Int = DEFAULT_TIMEOUT): Future[Either[Throwable, ItemTag]] = {
    import us.pinguo.bigdata.api.PhotoTaggingAPI.context
    implicit val formatter = DefaultFormats
    val headers = signature.header(requestURL, body, "PUT", "*/*", "*/*")
    val response = http(requestURL)
      .headers(headers.toArray: _*)
      .putByte(body)
      .requestForString

    response.map {
      case Left(e) => Left(e)
      case Right(json) => Right(Serialization.read[ItemTag](json))
    }
  }
}

object DataPlusItem {

  val PATTERN_ITEM_URL = "https://shujuapi.aliyun.com/%s/face/imgupload/upload?x-oss-process=udf/visual/detect"

}
