package messages

import play.api.libs.json.Json

/**
 * Created by trupin on 10/20/14.
 */
class MessageEnvelop(val message: Message, val bytes: Option[Array[Byte]])

object MessageEnvelop {
  def read(t: Int, headerBytes: Array[Byte], bytes: Option[Array[Byte]]): Option[MessageEnvelop] = t match {
    case InsertMessage.typeValue => Some(new MessageEnvelop(InsertMessage.InsertMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case RemoveMessage.typeValue => Some(new MessageEnvelop(RemoveMessage.RemoveMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case ReplaceMessage.typeValue => Some(new MessageEnvelop(ReplaceMessage.ReplaceMessageReads.reads(Json.parse(headerBytes)).get, bytes))
    case _ => None
  }
}