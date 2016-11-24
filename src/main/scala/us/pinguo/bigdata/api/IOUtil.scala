package us.pinguo.bigdata.api

import java.io.ByteArrayOutputStream
import java.net.{URL, URLConnection}

import org.apache.commons.io.IOUtils

trait IOUtil {

  def readRemoteToBuffer(url: String, timeOut: Int = 5000) = {
    val conn: URLConnection = new URL(url).openConnection()
    conn.setConnectTimeout(timeOut)
    conn.setReadTimeout(timeOut)
    conn.connect()
    val boStream: ByteArrayOutputStream = new ByteArrayOutputStream()
    IOUtils.copy(conn.getInputStream, boStream)
    boStream.close()
    boStream.toByteArray
  }

}
