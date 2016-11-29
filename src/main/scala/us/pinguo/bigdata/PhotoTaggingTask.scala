package us.pinguo.bigdata

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import us.pinguo.bigdata.api.PhotoTaggingAPI.{ExifTag, FaceTag, ItemTag, TaggingResult}
import us.pinguo.bigdata.dataplus._
import akka.pattern._
import com.ning.http.client.Response
import us.pinguo.bigdata.PhotoTaggingTask._
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys


class PhotoTaggingTask extends Actor with ActorLogging {

  private var receipt: ActorRef = ActorRef.noSender
  private var processingPhotoEtag: String = _

  private val signature = new DataPlusSignature(DataPlusKeys(accessID, accessKey))

  private var results = Map[ActorRef, Option[TaggingResult]]()

  override def receive: Receive = processDownload.orElse {
    case exifTag: ExifTag => results += (sender() -> exifTag)
    case faceTag: FaceTag => results += (sender() -> faceTag)
    case itemTag: ItemTag => results += (sender() -> itemTag)
  }

  private def processDownload: Receive = {
    case TaggingPhoto(etag) =>
      receipt = sender()
      processingPhotoEtag = etag
      download(processingPhotoEtag)

    case Left(throwable: Throwable) =>
      log.error(throwable, s"error when downloading image [$processingPhotoEtag]")
      context.stop(self)

    case Right(response: Response) if response.getStatusCode == 503 => download(processingPhotoEtag)

    case Right(response: Response) if response.getStatusCode == 200 =>
      val body = response.getResponseBodyAsBytes
      val faceActor = context.actorOf(DataPlusFaceActor.props(signature, organizationCode))
      val itemActor = context.actorOf(DataPlusItemActor.props(signature, organizationCode))
      val exifActor = context.actorOf(ExifRetrieveActor.props())

      results += faceActor -> null
      results += itemActor -> null
      results += exifActor -> null

      faceActor ! body
      itemActor ! body
      exifActor ! photo(processingPhotoEtag)
  }

  private def download(etag: String) = pipe(http(photo(etag)).request) to self

  private def photo(etag: String) = s"http://dn-phototask.qbox.me/$etag?imageView2/0/h/600"

  override def postStop() = {

  }
}

object PhotoTaggingTask {

  var accessID: String = _
  var accessKey: String = _
  var organizationCode: String = _

  case class TaggingPhoto(etag: String)

}