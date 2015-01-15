package messages

import models.{Buckets, Bucket}
import play.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._


/**
 * Created by trupin on 10/20/14.
 */
case class UserConnectionFileAction(bucketId: String,
                                    filePath: String,
                                    sessionToken: Option[String],
                                    userEmail: String) extends FileAction
{
  val typeValue = UserConnectionFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit = {
    broadcast(this.copy(sessionToken = None), None)
  }
}

object UserConnectionFileAction {
  import lib.util.Implicits._

  val typeValue = 6

  implicit val reads: Reads[UserConnectionFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "userEmail").read[String]
    )(UserConnectionFileAction.apply _)

  implicit val writes: Writes[UserConnectionFileAction] = new Writes[UserConnectionFileAction] {
    override def writes(o: UserConnectionFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "userEmail" -> o.userEmail
    )
  }
}