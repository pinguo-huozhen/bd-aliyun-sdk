package us.pinguo.bigdata.api

import java.util.regex.Pattern

import org.json4s.DefaultFormats
import us.pinguo.bigdata.dataplus.{DataPlusFace, DataPlusItem, DataPlusSignature, ExifRetrieve}
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys
import org.json4s._
import org.json4s.jackson.JsonMethods._
import us.pinguo.bigdata.api.PhotoTaggingAPI._

class PhotoTaggingAPI(access_id: String, access_secret: String, organize_code: String) extends IOUtil {
  implicit val formatter = DefaultFormats

  val signature = new DataPlusSignature(DataPlusKeys(access_id, access_secret))
  val faceHandler = new DataPlusFace(signature, organize_code)
  val itemHandler = new DataPlusItem(signature, organize_code)
  val exifHandler = new ExifRetrieve()

  def tagging(imageUrl: String, timeOut: Int = 5000): TaggingResponse = {
    //to do Exception
    val body = readRemoteToBuffer(imageUrl, timeOut)
    var faceTag: FaceTag = null
    var itemTag: ItemTag = null
    var exifTag: ExifTag = null

    val face = faceHandler.faceDetect(body, timeOut)
    if (face.code == SUCCESS_CODE) {
      val json = parse(face.json)
      var jsonString = compact(render((json \ "outputs") (0) \ "outputValue" \ "dataValue"))
      jsonString = jsonString.substring(1, jsonString.indexOf("\\n")).replaceAll(Pattern.quote("\\"), "")

      val faceResponse = parse(jsonString).extract[FaceResponse]
      if (faceResponse.errno == 0) {
        faceTag = FaceTag(faceResponse.age, faceResponse.gender, faceResponse.landmark, faceResponse.number, faceResponse.rect.sliding(4, 4).toList)
      }
    }

    val item = itemHandler.itemDetect(body, timeOut)
    if (item.code == SUCCESS_CODE) {
      itemTag = parse(item.json).extract[ItemTag]
    }

    val exif = exifHandler.getExif(s"$imageUrl&exif", timeOut)
    if (exif.code == SUCCESS_CODE) {
      exifTag = parse(exif.json).extract[ExifTag]
    }
    TaggingResponse(faceTag, itemTag, exifTag)
  }
}

object PhotoTaggingAPI {

  case class Bbox(height: Float, width: Float, xmin: Float, ymin: Float)

  case class Annotation(`class`: String, score: String, bbox: Bbox)

  case class Tag(confidence: String, value: String)

  case class ItemTag(annotation: List[Annotation] = Nil, tags: List[Tag] = Nil)

  case class FaceResponse(age: List[Int] = Nil, errno: Int = 0, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[Int] = Nil)

  case class FaceTag(age: List[Int] = Nil, gender: List[Int] = Nil, landmark: List[Float] = Nil, number: Int = 0, rect: List[List[Int]] = Nil)

  case class ExifInfo(`val`: String = null, `type`: Int = 0)

  case class ExifTag(YResolution: ExifInfo, ResolutionUnit: ExifInfo, Orientation: ExifInfo,
                     ColorSpace: ExifInfo, FlashPixVersion: ExifInfo, DateTime: ExifInfo,
                     ExifVersion: ExifInfo, XResolution: ExifInfo)

  case class TaggingResponse(face: FaceTag = null, item: ItemTag = null, exif: ExifTag = null)

}
