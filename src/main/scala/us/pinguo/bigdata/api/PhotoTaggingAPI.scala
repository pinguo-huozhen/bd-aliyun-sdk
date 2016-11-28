package us.pinguo.bigdata.api

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.imageio.ImageIO

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, _}
import us.pinguo.bigdata.api.PhotoTaggingAPI._
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import us.pinguo.bigdata.dataplus.{DataPlusFace, DataPlusItem, DataPlusSignature}
import us.pinguo.bigdata.http

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class PhotoTaggingAPI(access_id: String, access_secret: String, organize_code: String) extends IOUtil with Serializable {

  def tagging(imageUrl: String): Future[TaggingResponse] = {
    implicit val formatter = DefaultFormats
    import PhotoTaggingAPI._

    val signature = new DataPlusSignature(DataPlusKeys(access_id, access_secret))
    val faceHandler = new DataPlusFace(signature, organize_code)
    val itemHandler = new DataPlusItem(signature, organize_code)

    var errors = Map[String, String]()

    val body = try http(imageUrl).requestForBytes catch {
      case e: Throwable =>
        errors += ("download_image" -> e.getMessage)
        null
    }

    val stream = new ByteArrayInputStream(body)
    val img: BufferedImage = ImageIO.read(stream)

    val exifUrl: String = if (imageUrl.contains("?")) s"$imageUrl&exif" else s"$imageUrl?exif"

    val response = for {
      faceTag <- Future {
        try if (!errors.contains("download_image")) faceHandler.request(body) else null
        catch {
          case e: Throwable => errors += ("face_api" -> e.getMessage)
            null
        }
      }
      itemTag <- Future {
        try if (!errors.contains("download_image")) itemHandler.request(body) else null
        catch {
          case e: Throwable =>
            errors += ("item_api" -> e.getMessage)
            null
        }
      }
      exifTag <- Future {
        try parse(http(exifUrl).requestForString).extract[ExifTag]
        catch {
          case e: Throwable =>
            errors += ("exif_api" -> e.getMessage)
            null
        }
      }
    } yield TaggingResponse(faceTag, itemTag, exifTag, imageCalWH = ImageWH(img.getWidth, img.getHeight), errors.map(x => s"${x._1}=${x._2}").mkString(", "))

    response
  }

}

object PhotoTaggingAPI {

  implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(128))

  case class Bbox(height: Float, width: Float, xmin: Float, ymin: Float)

  case class Annotation(`class`: String, score: Float, bbox: Bbox)

  case class Tag(confidence: Float, value: String)

  case class ItemTag(annotation: List[Annotation] = Nil, tags: List[Tag] = Nil)

  case class FaceResponse(age: List[Int] = Nil, errno: Int = 0, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[Int] = Nil)

  case class FaceTag(age: List[Int] = Nil, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[List[Int]] = Nil)

  case class ExifInfo(`val`: String = null, `type`: Int = 0)

  case class ExifTag(YResolution: ExifInfo, ResolutionUnit: ExifInfo, Orientation: ExifInfo,
                     ColorSpace: ExifInfo, FlashPixVersion: ExifInfo, DateTime: ExifInfo,
                     ExifVersion: ExifInfo, XResolution: ExifInfo)

  case class ImageWH(width: Int, height: Int)

  case class TaggingResponse(face: FaceTag = null, item: ItemTag = null, exif: ExifTag = null, imageCalWH: ImageWH = null, error_message: String = null)

  case class PhotoTaggingException(code: Int, msg: String) extends Exception {
    override def getMessage: String = s"$code - $msg"
  }

}
