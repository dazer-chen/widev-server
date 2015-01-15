package messages

import managers.{FileManager, BucketManager}
import models.{Buckets, NavigatorElement, File, Bucket}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONDocument
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 1/13/15.
 */
case class AddFileAction(bucketId: String,
                         filePath: String,
                         sessionToken: Option[String],
                         files: Array[File],
                         navigator: Array[NavigatorElement]) extends FileAction
{
  val typeValue = AddFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit = {

    buckets.collection.update(BSONDocument("_id" -> bucket._id), BSONDocument(
      "$set" -> BSONDocument(
        "files" -> files,
        "navigator" -> navigator
      )
    )).map {
      case res if res.updated > 0 =>
        FileManager.delete(bucket, filePath)
        FileManager.insert(bucket, filePath, 0, bytes.get).map {
          case true =>
            broadcast(this.copy(sessionToken = None), bytes)
          case false =>
            Logger.warn("Couldn't add file")
        }
      case _ =>
        Logger.warn("Couldn't add file")
    }

  }
}

object AddFileAction {
  import lib.util.Implicits._

  val typeValue = 3

  implicit val reads: Reads[AddFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "files").read[Array[File]] and
      (JsPath \ "navigator").read[Array[NavigatorElement]]
    )(AddFileAction.apply _)

  implicit val writes: Writes[AddFileAction] = new Writes[AddFileAction] {
    override def writes(o: AddFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "files" -> o.files,
      "navigator" -> o.navigator
    )
  }
}