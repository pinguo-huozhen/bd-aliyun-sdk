package us.pinguo.bigdata

import dispatch._
import Defaults._
import com.ning.http.client.Response
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object http {
  def apply(uri: String, retires: Int = 5, timeout: FiniteDuration = 10 seconds): HttpRequest = new HttpRequest(uri, retires, timeout)
}

class HttpRequest(uri: String, retires: Int = 5, timeout: FiniteDuration = 10 seconds) {
  private val client = Http.configure { config =>
    config
      .setConnectionTimeoutInMs(timeout.toMillis.toInt)
      .setRequestTimeoutInMs(timeout.toMillis.toInt)
      .setUseRawUrl(true)
  }
  private var req = url(uri)

  def query(parameters: (String, String)*): HttpRequest = {
    parameters.foreach(x => req = req.addQueryParameter(x._1, x._2))
    this
  }

  def headers(properties: (String, String)*): HttpRequest = {
    properties.foreach(x => req = req.setHeader(x._1, x._2))
    this
  }

  def postByte(body: Array[Byte]): HttpRequest = {
    req = req.POST
    req = req.setBody(body)
    this
  }

  def putByte(body: Array[Byte]): HttpRequest = {
    req = req.PUT
    req = req.setBody(body)
    this
  }

  def postJson(body: AnyRef): HttpRequest = {
    postJson(Serialization.write(body)(DefaultFormats))
  }

  def postJson(body: String): HttpRequest = {
    req = req.POST
    req = req.setHeader("content-type", "application/json")
    req = req << body
    this
  }

  def postString(body: String): HttpRequest = {
    req = req.POST
    req = req << body
    this
  }

  def request: Response = {
    val start = System.currentTimeMillis()
    var isNeedRetry = false
    var response: Response = null
    do {
      Await.result(client(req).either, timeout) match {
        case Left(e) =>
          e.printStackTrace()
          throw e
        case Right(r) => r.getStatusCode match {
          case 200 => response = r
          case 503 => isNeedRetry = true
          case other_error => throw HttpStatusCodeError(other_error, r.getResponseBody)
        }
      }
    } while (isNeedRetry)
    val cost = System.currentTimeMillis() - start
    println(s"debug -> request [${req.toRequest.getRawUrl}] cost [$cost] ms")
    response
  }

  def requestForBytes: Array[Byte] = request.getResponseBodyAsBytes

  def requestForString: String = {
    try request.getResponseBody catch {
      case HttpStatusCodeError(404, _) => ""
    }
  }
}

case class HttpStatusCodeError(code: Int, body: String) extends RuntimeException {
  override def getMessage: String = s"http status code error [$code], body:$body"
}