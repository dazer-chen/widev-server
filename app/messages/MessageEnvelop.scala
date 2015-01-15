package messages

import play.api.libs.json.Json

/**
 * Created by trupin on 10/20/14.
 */
class MessageEnvelop(val message: FileAction, val bytes: Option[Array[Byte]])

object MessageEnvelop {
  def read(t: Int, headerBytes: Array[Byte], bytes: Option[Array[Byte]]): Option[MessageEnvelop] = t match {
    case InsertFileAction.typeValue => Some(new MessageEnvelop(InsertFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case RemoveFileAction.typeValue => Some(new MessageEnvelop(RemoveFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case ReplaceFileAction.typeValue => Some(new MessageEnvelop(ReplaceFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case AddFileAction.typeValue => Some(new MessageEnvelop(AddFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case DeleteFileAction.typeValue => Some(new MessageEnvelop(DeleteFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case UserPositionFileAction.typeValue => Some(new MessageEnvelop(UserPositionFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case UserConnectionFileAction.typeValue => Some(new MessageEnvelop(UserConnectionFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case UserDisconnectionFileAction.typeValue => Some(new MessageEnvelop(UserDisconnectionFileAction.reads.reads(Json.parse(headerBytes)).get, bytes))
    case _ => None
  }

  def write(action: FileAction) = action match {
    case a: InsertFileAction => Some(InsertFileAction.writes.writes(a))
    case a: RemoveFileAction => Some(RemoveFileAction.writes.writes(a))
    case a: ReplaceFileAction => Some(ReplaceFileAction.writes.writes(a))
    case a: AddFileAction => Some(AddFileAction.writes.writes(a))
    case a: DeleteFileAction => Some(DeleteFileAction.writes.writes(a))
    case a: UserPositionFileAction => Some(UserPositionFileAction.writes.writes(a))
    case a: UserDisconnectionFileAction => Some(UserDisconnectionFileAction.writes.writes(a))
    case _ => None
  }
}