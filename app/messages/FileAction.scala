package messages

import models.Bucket

/**
 * Created by trupin on 10/20/14.
 */
trait FileAction {
  val typeValue: Int
  val bucketId: String
  val filePath: String
  val sessionToken: Option[String]

  def action(bucket: Bucket, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit
}
