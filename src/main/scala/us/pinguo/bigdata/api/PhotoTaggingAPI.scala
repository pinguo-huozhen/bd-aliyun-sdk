package us.pinguo.bigdata.api

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.imageio.ImageIO

import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, _}
import us.pinguo.bigdata.api.PhotoTaggingAPI._
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import us.pinguo.bigdata.dataplus.{DataPlusFaceActor$, DataPlusItemActor$, DataPlusSignature}
import us.pinguo.bigdata.http

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class PhotoTaggingAPI(access_id: String, access_secret: String, organize_code: String) extends IOUtil with Serializable {

  def tagging(imageUrl: String): Future[TaggingResponse] = {
    implicit val formatter = DefaultFormats
    import PhotoTaggingAPI._

    val signature = new DataPlusSignature(DataPlusKeys(access_id, access_secret))
    val faceHandler = new DataPlusFaceActor(signature, organize_code)
    val itemHandler = new DataPlusItemActor(signature, organize_code)

    var errors = Map[String, String]()

    val imageDownloadFuture = http(imageUrl).requestForBytes.map {
      case Left(_) => null
      case Right(bytes: Array[Byte]) => bytes
    }
    val body = Await.result(imageDownloadFuture, 10 seconds)

    if (body != null) {
      val exifUrl: String = if (imageUrl.contains("?")) s"$imageUrl&exif" else s"$imageUrl?exif"
      val stream = new ByteArrayInputStream(body)
      val img: BufferedImage = ImageIO.read(stream)
      for {
        faceTag <- faceHandler.request(body) map {
          case Left(e: Throwable) =>
            errors += ("face_api" -> e.getMessage)
            null
          case Right(tag) => tag
        }
        itemTag <- itemHandler.request(body) map {
          case Left(e: Throwable) =>
            errors += ("item_api" -> e.getMessage)
            null
          case Right(tag) => tag
        }
        exifTag <- http(exifUrl).requestForString map {
          case Left(e: Throwable) =>
            errors += ("exif_api" -> e.getMessage)
            null
          case Right(json: String) =>
            try Serialization.read[ExifTag](json) catch {
              case e: Exception =>
                errors += ("exif_api" -> (e.getMessage + " " + json))
                null
            }
        }
      } yield TaggingResponse(faceTag, itemTag, exifTag, imageCalWH = ImageWH(img.getWidth, img.getHeight), errors.map(x => s"${x._1}=${x._2}").mkString(", "))
    } else Future(TaggingResponse(null, null, null, null, "image_download->failed"))
  }

}

object PhotoTaggingAPI {

  implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(600))

  case class Bbox(height: Float, width: Float, xmin: Float, ymin: Float)

  case class Annotation(`class`: String, score: Float, bbox: Bbox)

  case class Tag(confidence: Float, value: String)

  case class ItemTag(annotation: List[Annotation] = Nil, tags: List[Tag] = Nil)

  case class FaceResponse(age: List[Int] = Nil, errno: Int = 0, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[Int] = Nil)

  case class FaceTag(age: List[Int] = Nil, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[List[Int]] = Nil)

  case class ExifInfo(`val`: String = null, `type`: Int = 0)

  case class ExifTag(YResolution: Option[ExifInfo], ResolutionUnit: Option[ExifInfo], Orientation: Option[ExifInfo],
                     ColorSpace: Option[ExifInfo], FlashPixVersion: Option[ExifInfo], DateTime: Option[ExifInfo],
                     ExifVersion: Option[ExifInfo], XResolution: Option[ExifInfo])

  case class ImageWH(width: Int, height: Int)

  case class TaggingResponse(face: FaceTag = null, item: ItemTag = null, exif: ExifTag = null, imageCalWH: ImageWH = null, error_message: String = null)

  case class PhotoTaggingException(code: Int, msg: String) extends Exception {
    override def getMessage: String = s"$code - $msg"
  }

}
