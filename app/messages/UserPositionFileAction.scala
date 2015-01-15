package messages

import models.{Buckets, Bucket}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Created by trupin on 1/13/15.
 */
case class UserPositionFileAction(bucketId: String,
                                  filePath: String,
                                  sessionToken: Option[String],
                                  position: Int,
                                  userEmail: String) extends FileAction
{
  override val typeValue: Int = UserPositionFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit)(buckets: Buckets): Unit = {
    broadcast(this.copy(sessionToken = None), None)
  }
}

object UserPositionFileAction {
  val typeValue = 5

  implicit val reads: Reads[UserPositionFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "position").read[Int] and
      (JsPath \ "userEmail").read[String]
    )(UserPositionFileAction.apply _)

  implicit val writes: Writes[UserPositionFileAction] = new Writes[UserPositionFileAction] {
    override def writes(o: UserPositionFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "position" -> o.position,
      "userEmail" -> o.userEmail
    )
  }
}

