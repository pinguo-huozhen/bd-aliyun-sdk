package us.pinguo.bigdata.api

import us.pinguo.bigdata.dataplus.{DataPlusFace, DataPlusItem, DataPlusSignature, ExifRetrieve}
import us.pinguo.bigdata.dataplus.DataPlusSignature.DataPlusKeys


class PhotoTaggingAPI(access_id: String, access_secret: String, organize_code: String) extends IOUtil {

  val signature = new DataPlusSignature(DataPlusKeys(access_id, access_secret))
  val faceHandler = new DataPlusFace(signature, organize_code)
  val itemHandler = new DataPlusItem(signature, organize_code)
  val exifHandler = new ExifRetrieve()

  def tagging(imageUrl: String, timeOut: Int = 5000) = {
    val body = readRemoteToBuffer(imageUrl, timeOut)

    val face = faceHandler.faceDetect(body)
    val item = itemHandler.itemDetect(body)
    val exif = exifHandler.getExif(s"$imageUrl&exif")
  }
}
