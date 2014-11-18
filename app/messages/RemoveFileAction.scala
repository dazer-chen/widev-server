package messages

import models.FileCaches
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/20/14.
 */
case class RemoveFileAction(fd: String, sessionToken: Option[String], at: Int, length: Int) extends FileAction {
  override val typeValue: Int = RemoveFileAction.typeValue

  override def action(sender: BSONObjectID, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit =
    if (FileCaches.remove(fd, sender, at, length))
      broadcast(InsertFileAction(fd, None, at), None)
}

object RemoveFileAction {
  val typeValue = 1

  implicit val RemoveMessageReads: Reads[RemoveFileAction] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int] and
      (JsPath \ "length").read[Int]
    )(RemoveFileAction.apply _)

  implicit val RemoveMessageWrites: Writes[RemoveFileAction] = new Writes[RemoveFileAction] {
    override def writes(o: RemoveFileAction): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at,
      "length" -> o.length
    )
  }
}