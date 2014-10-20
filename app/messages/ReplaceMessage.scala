package messages

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by trupin on 10/20/14.
 */
case class ReplaceMessage(fd: String, sessionToken: String, at: Int) extends Message {
  override val typeValue: Int = 2
}


object ReplaceMessage {
  val typeValue = 2

  implicit val ReplaceMessageReads: Reads[ReplaceMessage] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[String] and
      (JsPath \ "at").read[Int]
    )(ReplaceMessage.apply _)

  implicit val ReplaceMessageWrites: Writes[ReplaceMessage] = new Writes[ReplaceMessage] {
    override def writes(o: ReplaceMessage): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}
