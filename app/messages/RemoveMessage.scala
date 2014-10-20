package messages

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by trupin on 10/20/14.
 */
case class RemoveMessage(fd: String, sessionToken: String, at: Int, length: Int) extends Message {
  override val typeValue: Int = RemoveMessage.typeValue
}

object RemoveMessage {
  val typeValue = 1

  implicit val RemoveMessageReads: Reads[RemoveMessage] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[String] and
      (JsPath \ "at").read[Int] and
      (JsPath \ "length").read[Int]
    )(RemoveMessage.apply _)

  implicit val RemoveMessageWrites: Writes[RemoveMessage] = new Writes[RemoveMessage] {
    override def writes(o: RemoveMessage): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at,
      "length" -> o.length
    )
  }
}