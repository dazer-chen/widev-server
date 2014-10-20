package messages

import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/20/14.
 */
trait FileAction {
  val typeValue: Int
  val fd: String
  val sessionToken: Option[String]

  def action(sender: BSONObjectID, bytes: Option[Array[Byte]], broadcast: (FileAction, Option[Array[Byte]]) => Unit): Unit
}
