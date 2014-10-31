package managers

import java.nio.ByteBuffer

import messages.{FileAction, MessageEnvelop}
import models.FileCaches
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/31/14.
 */
class BucketManager {
  val channelPerUser = scala.collection.mutable.LinkedHashMap.empty[String, Channel[Array[Byte]]]

  def broadCastMessageToFileRoom(message: FileAction, bytes: Option[Array[Byte]], sender: BSONObjectID) =
    writeMessage(message, bytes) match {
      case Some(bytesToSend) =>
        channelPerUser.synchronized {
          (FileCaches.users(message.fd) - sender).foreach {
            case id if channelPerUser.get(id.stringify).nonEmpty =>
              channelPerUser(id.stringify).push(bytesToSend)
            case id =>
              Logger.warn(s"No openned channel for user: '$id'")
          }
        }
      case _ =>
    }

  def readMessage(bytes: Array[Byte]): Option[MessageEnvelop] = {
    try {
      val headerSize = ByteBuffer.wrap(bytes.slice(0, 4)).getInt
      val messageType = ByteBuffer.wrap(bytes.slice(4, 8)).getInt
      val messageBytes = bytes.slice(8, headerSize + 8)
      MessageEnvelop.read(messageType, messageBytes, if (bytes.size > headerSize + 8)
        Some(bytes.slice(headerSize + 8, bytes.size))
      else
        None
      )
    } catch {
      case e: Exception =>
        Logger.error(e.getMessage, e.getCause)
        None
    }
  }

  def writeMessage(message: FileAction, bytes: Option[Array[Byte]]): Option[Array[Byte]] = {
    MessageEnvelop.write(message) match {
      case Some(json) =>
        val jsonBytes = json.toString().getBytes
        val headerSize = jsonBytes.size
        val buffer = ByteBuffer.allocate(8)
        buffer.putInt(headerSize)
        buffer.putInt(message.typeValue)
        Some(buffer.array ++ jsonBytes ++ (if (bytes.nonEmpty) bytes.get else Array.empty[Byte]))
      case None => None
    }
  }

}

object BucketManager extends BucketManager