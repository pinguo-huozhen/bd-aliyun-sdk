package us.pinguo.bigdata

import java.util.concurrent.Executors

import akka.actor.Actor

import scala.concurrent.ExecutionContext


abstract class DataPlusActor extends Actor {

  val SUCCESS_CODE = 200

  val SERVER_BUSY = 503

  val FATAL_CODE = 400

}

object DataPlusActor {

  implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(600))

  trait TaggingResult

  case class ItemTag(annotation: List[Annotation] = Nil, tags: List[Tag] = Nil) extends TaggingResult

  case class FaceTag(age: List[Int] = Nil, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[List[Int]] = Nil) extends TaggingResult

  case class ExifTag(YResolution: Option[ExifInfo], ResolutionUnit: Option[ExifInfo], Orientation: Option[ExifInfo],
                     ColorSpace: Option[ExifInfo], FlashPixVersion: Option[ExifInfo], DateTime: Option[ExifInfo],
                     ExifVersion: Option[ExifInfo], XResolution: Option[ExifInfo]) extends TaggingResult

  case class Bbox(height: Float, width: Float, xmin: Float, ymin: Float)

  case class Annotation(`class`: String, score: Float, bbox: Bbox)

  case class Tag(confidence: Float, value: String)

  case class FaceResponse(age: List[Int] = Nil, errno: Int = 0, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[Int] = Nil)

  case class ExifInfo(`val`: String = null, `type`: Int = 0)

  case class ImageWH(width: Int, height: Int)

  case class TaggingResponse(face: FaceTag = null, item: ItemTag = null, exif: ExifTag = null, imageCalWH: ImageWH = null, error_message: String = null)

  case class PhotoTaggingException(code: Int, msg: String) extends Exception {
    override def getMessage: String = s"$code - $msg"
  }

}
