package messages

import models.FileCaches
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/20/14.
 */
case class ReplaceFileAction(fd: String, sessionToken: Option[String], at: Int) extends FileAction {
  override val typeValue: Int = 2

  override def action(sender: BSONObjectID, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit =
    if (bytes.nonEmpty && FileCaches.replace(fd, sender, at, bytes.get))
      broadcast(ReplaceFileAction(fd, None, at), bytes)
}


object ReplaceFileAction {
  val typeValue = 2

  implicit val ReplaceMessageReads: Reads[ReplaceFileAction] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int]
    )(ReplaceFileAction.apply _)

  implicit val ReplaceMessageWrites: Writes[ReplaceFileAction] = new Writes[ReplaceFileAction] {
    override def writes(o: ReplaceFileAction): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}
