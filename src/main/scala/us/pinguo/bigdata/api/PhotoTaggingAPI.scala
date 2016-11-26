package us.pinguo.bigdata.api

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.regex.Pattern
import javax.imageio.ImageIO
import org.json4s.DefaultFormats
import us.pinguo.bigdata.dataplus.{DataPlusFace, DataPlusItem, DataPlusSignature, ExifRetrieve}
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import org.json4s._
import org.json4s.jackson.JsonMethods._
import us.pinguo.bigdata.api.PhotoTaggingAPI._

class PhotoTaggingAPI(access_id: String, access_secret: String, organize_code: String) extends IOUtil with Serializable {

  val signature = new DataPlusSignature(DataPlusKeys(access_id, access_secret))
  val faceHandler = new DataPlusFace(signature, organize_code)
  val itemHandler = new DataPlusItem(signature, organize_code)
  val exifHandler = new ExifRetrieve()

  def tagging(imageUrl: String, timeOut: Int = 10000, retryTimes:Int = DEFAULT_RETRY): TaggingResponse = {
    implicit val formatter = DefaultFormats
    var faceTag: FaceTag = null
    var itemTag: ItemTag = null
    var exifTag: ExifTag = null
    var imageWH: ImageWH = null
    try {
      val body = loadImage(imageUrl, timeOut, retryTimes)
      if(body.isEmpty) throw PhotoTaggingException(FATAL_CODE, s"can not load image:[$imageUrl]")

      val img: BufferedImage = ImageIO.read(new ByteArrayInputStream(body))
      imageWH = ImageWH(img.getWidth, img.getHeight)

      val exifUrl:String = if(imageUrl.contains("?")) s"$imageUrl&exif" else s"$imageUrl?exif"
//      val face = retry(faceHandler.request(body, timeOut))
//      val item = retry(itemHandler.request(body, timeOut))
//      val exif = retry(exifHandler.request(exifUrl, timeOut))
//      val (face:ImageResponse, item:ImageResponse, exif:ImageResponse) = for {
//        face <- retry(faceHandler.request(body, timeOut))
//        item <- retry(itemHandler.request(body, timeOut))
//        exif <- retry(exifHandler.request(exifUrl, timeOut))
//      } yield (face, item, exif)

      val face = retry(faceHandler.request(body, timeOut), retryTimes)
      if (face.code == SUCCESS_CODE) {
        val json = parse(face.json)
        var jsonString = compact(render((json \ "outputs") (0) \ "outputValue" \ "dataValue"))
        jsonString = jsonString.substring(1, jsonString.indexOf("\\n")).replaceAll(Pattern.quote("\\"), "")

        val faceResponse = parse(jsonString).extract[FaceResponse]
        if (faceResponse.errno == 0) {
          faceTag = FaceTag(faceResponse.age, faceResponse.gender, faceResponse.landmark, faceResponse.number, faceResponse.rect.sliding(4, 4).toList)
        } else throw PhotoTaggingException(face.code, s"face response errno [${faceResponse.errno}]")
      } else throw PhotoTaggingException(face.code, s"face response: [${face.json}]")

      val item = retry(itemHandler.request(body, timeOut), retryTimes)
      if (item.code == SUCCESS_CODE) {
        itemTag = parse(item.json).extract[ItemTag]
      } else throw PhotoTaggingException(item.code, s"item response: [${item.json}]")

      val exif = retry(exifHandler.request(exifUrl, timeOut), retryTimes)
      if (exif.code == SUCCESS_CODE) {
        exifTag = parse(exif.json).extract[ExifTag]
      } else throw PhotoTaggingException(exif.code, s"exif response: [${exif.json}]")
    } catch {
      case pex: PhotoTaggingException => throw pex
      case ex: Exception => throw PhotoTaggingException(500, ex.getMessage)
    }
    TaggingResponse(faceTag, itemTag, exifTag, imageWH)
  }
}

object PhotoTaggingAPI {

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

  case class TaggingResponse(face: FaceTag = null, item: ItemTag = null, exif: ExifTag = null, imageCalWH: ImageWH = null, error_message:String = null)

  case class PhotoTaggingException(code: Int, msg: String) extends Exception {
    override def getMessage: String = s"$code - $msg"
  }
}
