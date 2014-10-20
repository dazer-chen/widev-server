package messages

import play.api.libs.json.Json

/**
 * Created by trupin on 10/20/14.
 */
class MessageEnvelop(val message: FileAction, val bytes: Option[Array[Byte]])

object MessageEnvelop {
  def read(t: Int, headerBytes: Array[Byte], bytes: Option[Array[Byte]]): Option[MessageEnvelop] = t match {
    case InsertFileAction.typeValue => Some(new MessageEnvelop(InsertFileAction.InsertMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case RemoveFileAction.typeValue => Some(new MessageEnvelop(RemoveFileAction.RemoveMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case ReplaceFileAction.typeValue => Some(new MessageEnvelop(ReplaceFileAction.ReplaceMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case _ => None
  }

  def write(action: FileAction) = action match {
    case a: InsertFileAction => Some(InsertFileAction.InsertMessageWrites.writes(a))
    case a: RemoveFileAction => Some(RemoveFileAction.RemoveMessageWrites.writes(a))
    case a: ReplaceFileAction => Some(ReplaceFileAction.ReplaceMessageWrites.writes(a))
    case _ => None
  }
}