package messages

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by trupin on 10/20/14.
 */
case class InsertMessage(fd: String,
                         sessionToken: String,
                         at: Int) extends Message
{
  val typeValue = InsertMessage.typeValue
}

object InsertMessage {
  val typeValue = 0

  implicit val InsertMessageReads: Reads[InsertMessage] = (
    (JsPath \ "fd").read[String] and
      (JsPath \ "sessionToken").read[String] and
      (JsPath \ "at").read[Int]
    )(InsertMessage.apply _)

  implicit val InsertMessageWrites: Writes[InsertMessage] = new Writes[InsertMessage] {
    override def writes(o: InsertMessage): JsValue = Json.obj(
      "fd" -> o.fd,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}