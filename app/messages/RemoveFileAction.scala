package messages

import managers.FileManager
import models.Bucket
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by trupin on 10/20/14.
 */
case class RemoveFileAction(bucketId: String, filePath: String, sessionToken: Option[String], at: Int, length: Int) extends FileAction {
  override val typeValue: Int = RemoveFileAction.typeValue

  // TODO write action
  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit =
    FileManager.delete(bucket, filePath, at, length).map {
      case true =>
        broadcast(copy(sessionToken = None), None)
      case _ =>
    }
}

object RemoveFileAction {
  val typeValue = 1

  implicit val RemoveMessageReads: Reads[RemoveFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int] and
      (JsPath \ "length").read[Int]
    )(RemoveFileAction.apply _)

  implicit val RemoveMessageWrites: Writes[RemoveFileAction] = new Writes[RemoveFileAction] {
    override def writes(o: RemoveFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at,
      "length" -> o.length
    )
  }
}