package messages

import models.{Buckets, Bucket}
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by trupin on 10/20/14.
 */
case class UserDisconnectionFileAction(bucketId: String,
                                       filePath: String,
                                       sessionToken: Option[String],
                                       userEmail: String) extends FileAction
{
  val typeValue = UserDisconnectionFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit = {
    broadcast(this.copy(sessionToken = None), None)
  }
}

object UserDisconnectionFileAction {
  import lib.util.Implicits._

  val typeValue = 7

  implicit val reads: Reads[UserDisconnectionFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "userEmail").read[String]
    )(UserDisconnectionFileAction.apply _)

  implicit val writes: Writes[UserDisconnectionFileAction] = new Writes[UserDisconnectionFileAction] {
    override def writes(o: UserDisconnectionFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "userEmail" -> o.userEmail
    )
  }
}