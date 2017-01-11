package us.pinguo.bigdata

import com.ning.http.client.Response
import dispatch.Defaults._
import dispatch._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

import scala.concurrent.duration._
import scala.language.postfixOps

object http {
  val client: Http = Http.configure { config =>
    config
      .setConnectionTimeoutInMs(15000)
      .setRequestTimeoutInMs(30000)
      .setUseRawUrl(true)
  }



  def apply(uri: String, retires: Int = 5, timeout: FiniteDuration = 15 seconds): HttpRequest = new HttpRequest(client, uri, retires, timeout)
}

class HttpRequest(client: Http, uri: String, retires: Int, timeout: FiniteDuration) {

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

  def request: Future[Either[Throwable, Response]] = {
    client(req).either map {
      case Left(e) => Left(e)
      case Right(response) => response.getStatusCode match {
        case 200 => Right(response)
        case 503 => Right(response)
        case other_error => Left(HttpStatusCodeError(other_error, response.getResponseBody))
      }
    }
  }

  def requestForBytes: Future[Either[Throwable, Array[Byte]]] = {

    request map {
      case Left(e) => Left(e)
      case Right(response) => Right(response.getResponseBodyAsBytes)
    }
  }

  def requestForString: Future[Either[Throwable, String]] = {
    request.map {
      case Left(e) => Left(e)
      case Right(response) if response.getResponseBody().nonEmpty => Right(response.getResponseBody)
      case _ => Right("")
    }
  }
}

case class HttpStatusCodeError(code: Int, body: String) extends RuntimeException {
  def getCode: Int = code
  override def getMessage: String = s"http status code error [$code], body:$body"
}