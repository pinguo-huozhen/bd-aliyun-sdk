package us.pinguo.bigdata

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import akka.pattern._
import com.ning.http.client.Response
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import us.pinguo.bigdata.DataPlusActor.{ExifTag, FaceTag, ImageWH, ItemTag, TaggingError, TaggingResponse, TaggingResult}
import us.pinguo.bigdata.PhotoTaggingTask._
import us.pinguo.bigdata.dataplus.DataPlusFaceActor.RequestFace
import us.pinguo.bigdata.dataplus.DataPlusItemActor.RequestItem
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import us.pinguo.bigdata.dataplus.ExifRetrieveActor.RequestExif
import us.pinguo.bigdata.dataplus._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.language.postfixOps


class PhotoTaggingTask extends Actor with ActorLogging {

  import context.dispatcher

  private var receipt: ActorRef = ActorRef.noSender
  private var processingPhotoEtag: String = _
  private var processingPhotoBody: Array[Byte] = _

  private val signature = new DataPlusSignature(DataPlusKeys(accessID, accessKey))
  private val faceActor = context.actorOf(DataPlusFaceActor.props(signature, organizationCode).withDispatcher("face-api-dispatcher"))
  private val itemActor = context.actorOf(DataPlusItemActor.props(signature, organizationCode).withDispatcher("item-api-dispatcher"))
  private val exifActor = context.actorOf(ExifRetrieveActor.props())
  private val terminate = context.system.scheduler.scheduleOnce(20 seconds, self, PoisonPill)
  private var cachedEtag: String = _

  private var results = Map[ActorRef, Either[String, TaggingResult]]()

  private val taskStarted = System.currentTimeMillis()

  override def aroundReceive(receive: Receive, msg: Any): Unit = {
    super.aroundReceive(receive, msg)
    if (results.size == 3) {
      val stream = new ByteArrayInputStream(processingPhotoBody)
      val img: BufferedImage = ImageIO.read(stream)
      receipt ! TaggingResponse(
        resultOf(faceActor),
        resultOf(itemActor),
        resultOf(exifActor),
        ImageWH(img.getWidth(), img.getHeight()),
        Seq(faceActor, itemActor, exifActor).map(errorOf).filter(_.nonEmpty).mkString(", ")
      )

      context.stop(self)
    }
  }


  override def postStop(): Unit = {
    log.info(s"task ${self.path.toStringWithoutAddress} etag:[$cachedEtag] completed in [${System.currentTimeMillis() - taskStarted}] ms")
    terminate.cancel()
  }

  override def receive: Receive = processDownload.orElse {
    case exifTag: ExifTag => results += (sender() -> Right(exifTag))
    case faceTag: FaceTag => results += (sender() -> Right(faceTag))
    case itemTag: ItemTag => results += (sender() -> Right(itemTag))
    case TaggingError(message) =>
      log.warning(s"meet tagging error:[$message] etag:[$cachedEtag] ")
      results += (sender() -> Left(message))
  }

  private def processDownload: Receive = {
    case TaggingPhoto(etag) =>
      cachedEtag = etag
      receipt = sender()
      processingPhotoEtag = etag
      download(processingPhotoEtag)

    case Left(throwable: Throwable) =>
      log.error(throwable, s"error when downloading image [$processingPhotoEtag]")
      context.stop(self)

    case Right(response: Response) if response.getStatusCode == 503 => download(processingPhotoEtag)

    case Right(response: Response) if response.getStatusCode == 200 =>
      processingPhotoBody = response.getResponseBodyAsBytes

      faceActor ! RequestFace(processingPhotoBody)
      itemActor ! RequestItem(processingPhotoBody)
      exifActor ! RequestExif(photo(processingPhotoEtag))
  }

  private def resultOf[T](key: ActorRef): T = {
    results(key) match {
      case Left(_) => null.asInstanceOf[T]
      case Right(tag) => tag.asInstanceOf[T]
    }
  }

  private def errorOf(key: ActorRef): String = {
    results(key) match {
      case Left(message) => s"${key.path.elements.last} -> $message"
      case Right(_) => ""
    }
  }

  private def download(etag: String) = pipe(http(photo(etag)).request) to self

  private def photo(etag: String) = s"http://dn-phototask.qbox.me/$etag?imageView2/0/h/600"
}

object PhotoTaggingTask {

  var accessID: String = _
  var accessKey: String = _
  var organizationCode: String = _

  case class TaggingPhoto(etag: String)

  implicit class TaggingProxy(proxyUrlTemplate: String) {
    def tagging(etag: String, timeout: FiniteDuration = 30 seconds)(implicit execution: ExecutionContext): TaggingResponse = {
      implicit val format = DefaultFormats
      try {
        val json = http(proxyUrlTemplate.format(etag)).requestForString map {
          case Left(e) => Serialization.write(Map("error_message" -> s"request_error -> ${e.getClass.getCanonicalName}"))
          case Right(s: String) => s
        }
        Serialization.read[TaggingResponse](Await.result(json, timeout))
      } catch {
        case e: Exception =>
          TaggingResponse(error_message = s"request_error -> ${e.getClass.getCanonicalName} - ${e.getMessage}")
      }
    }
  }

}