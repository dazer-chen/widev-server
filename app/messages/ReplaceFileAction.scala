package messages

import managers.FileManager
import models.{Buckets, Bucket}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 10/20/14.
 */
case class ReplaceFileAction(bucketId: String, filePath: String, sessionToken: Option[String], at: Int) extends FileAction {
  override val typeValue: Int = 2

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit =
    FileManager.replace(bucket, filePath, at, bytes.get).map {
      case true =>
        broadcast(copy(sessionToken = None), bytes)
      case _ =>
    }
}


object ReplaceFileAction {
  val typeValue = 2

  implicit val reads: Reads[ReplaceFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int]
    )(ReplaceFileAction.apply _)

  implicit val writes: Writes[ReplaceFileAction] = new Writes[ReplaceFileAction] {
    override def writes(o: ReplaceFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}
