package messages

import managers.FileManager
import models.{NavigatorElement, File, Buckets, Bucket}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 1/13/15.
 */
case class DeleteFileAction(bucketId: String,
                            filePath: String,
                            sessionToken: Option[String],
                            files: Array[File],
                            navigator: Array[NavigatorElement]) extends FileAction {
  override val typeValue: Int = DeleteFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit = {
    buckets.collection.update(BSONDocument("_id" -> bucket._id), BSONDocument(
      "$set" -> BSONDocument(
        "files" -> files,
        "navigator" -> navigator
      )
    )).map {
      case res if res.updated > 0 =>
        FileManager.delete(bucket, filePath)
        broadcast(this.copy(sessionToken = None), bytes)
      case _ =>
        Logger.warn("Couldn't add file")
    }
  }
}

object DeleteFileAction {
  val typeValue = 4

  implicit val reads: Reads[DeleteFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "files").read[Array[File]] and
      (JsPath \ "navigator").read[Array[NavigatorElement]]
    )(DeleteFileAction.apply _)

  implicit val writes: Writes[DeleteFileAction] = new Writes[DeleteFileAction] {
    override def writes(o: DeleteFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "files" -> o.files,
      "navigator" -> o.navigator
    )
  }
}
