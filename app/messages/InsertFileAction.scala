package messages

import managers.FileManager
import models.Bucket
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by trupin on 10/20/14.
 */
case class InsertFileAction(bucketId: String,
                            filePath: String,
                            sessionToken: Option[String],
                            at: Int) extends FileAction
{
  val typeValue = InsertFileAction.typeValue

  override def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit =
    FileManager.insert(bucket, filePath, at, bytes.get).map {
      case true =>
        broadcast(copy(sessionToken = None), bytes)
      case _ =>
    }

}

object InsertFileAction {
  import lib.util.Implicits._

  val typeValue = 0

  implicit val InsertMessageReads: Reads[InsertFileAction] = (
    (JsPath \ "bucketId").read[String] and
      (JsPath \ "filePath").read[String] and
      (JsPath \ "sessionToken").read[Option[String]] and
      (JsPath \ "at").read[Int]
    )(InsertFileAction.apply _)

  implicit val InsertMessageWrites: Writes[InsertFileAction] = new Writes[InsertFileAction] {
    override def writes(o: InsertFileAction): JsValue = Json.obj(
      "bucketId" -> o.bucketId,
      "filePath" -> o.filePath,
      "sessionToken" -> o.sessionToken,
      "at" -> o.at
    )
  }
}