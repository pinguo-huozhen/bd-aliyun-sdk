package us.pinguo.bigdata.dataplus

import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Base64, Calendar, Locale, TimeZone}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.digest.DigestUtils
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import us.pinguo.bigdata.dataplus.DataPlusSignature._


class DataPlusSignature(keys: DataPlusKeys) extends Serializable {

  def header(requestUrl: String, body: Array[Byte], method: String, accept: String = "json", contentType: String = "application/json") = {
    val gmtTime = currentGMTTime
    val signString = sign(requestUrl, body, method, accept, contentType, gmtTime)
    Map("accept" -> accept,
      "content-type" -> contentType,
      "date" -> gmtTime,
      "Authorization" -> s"Dataplus ${keys.access_id}:$signString")
  }

  def base64Encode(bytes: Array[Byte]): String = Base64.getEncoder.encodeToString(bytes)

  def md5(bytes: Array[Byte]): String = DigestUtils.md5Hex(bytes)

  private def sign(requestUrl: String, body: Array[Byte], method: String, accept: String, contentType: String, gmtTime: String) = {
    val md5Body = base64Encode(DigestUtils.md5(body))
    val urlStr = urlPath(requestUrl)
    val signString = s"$method\n$accept\n$md5Body\n$contentType\n$gmtTime\n$urlStr"
    base64Encode(hmac(keys.access_secret, signString))
  }

  private def hmac(key: String, data: String) = {
    val signKey: SecretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA1)
    val mac: Mac = Mac.getInstance(HMAC_SHA1)
    mac.init(signKey)
    mac.doFinal(data.getBytes())
  }

  private def urlPath(requestUrl: String) = {
    val url = new URL(requestUrl)
    if (requestUrl.contains("?")) s"${url.getPath}?${url.getQuery}"
    else url.getPath
  }

  private def currentGMTTime = {
    val format = new SimpleDateFormat("EEE d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
    format.setTimeZone(TimeZone.getTimeZone("GMT"))
    val time = Calendar.getInstance()
    format.format(time.getTime)
  }

}

object DataPlusSignature {

  val HMAC_SHA1 = "HmacSHA1"

  case class DataPlusKeys(access_id: String, access_secret: String)

}
