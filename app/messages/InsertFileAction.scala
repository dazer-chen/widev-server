package messages

import models.FileCaches
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/20/14.
 */
case class InsertFileAction(fd: String,
                            sessionToken: Option[String],
                            at: Int) extends FileAction
{
  val typeValue = InsertFileAction.typeValue

  override def action(sender: BSONObjectID, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit =
    if (bytes.nonEmpty && FileCaches.insert(fd, sender, at, bytes.get))
      broadcast(InsertFileAction(fd, None, at), bytes)

}

object InsertFileAction {
  val typeValue = 0

  implicit val InsertMessageReads: Reads[InsertFileAction] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int]
    )(InsertFileAction.apply _)

  implicit val InsertMessageWrites: Writes[InsertFileAction] = new Writes[InsertFileAction] {
    override def writes(o: InsertFileAction): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}