package us.pinguo.bigdata.api

import java.io.ByteArrayOutputStream
import java.net.{URL, URLConnection}

import org.apache.commons.io.IOUtils
import us.pinguo.bigdata.dataplus.DataPlusUtil

trait IOUtil extends DataPlusUtil {

  def loadImage(url: String, timeOut: Int = DEFAULT_TIMEOUT, retryTimes: Int = DEFAULT_RETRY):Array[Byte] = {
    var times = retryTimes
    var response: Array[Byte] = Array()
    do {
      response = readRemoteToBuffer(url, timeOut)
      if(response.nonEmpty) times = 0
      times -= 1
    } while(times>0)
    response
  }

  def readRemoteToBuffer(url: String, timeOut: Int = DEFAULT_TIMEOUT):Array[Byte] = {
    try {
      val conn: URLConnection = new URL(url).openConnection()
      conn.setConnectTimeout(timeOut)
      conn.setReadTimeout(timeOut)
      conn.connect()
      val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
      IOUtils.copy(conn.getInputStream, boStream)
      boStream.close()
      boStream.toByteArray
    } catch {
      case ex: Exception => Array()
    }
  }

}
