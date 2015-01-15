package managers

import java.nio.{ByteBuffer, ByteOrder}

import messages.{FileAction, MessageEnvelop}
import models._
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.concurrent.Execution.Implicits._

/**reci
 * Created by trupin on 10/31/14.
 */
class BucketManager {
  val channelPerUser = scala.collection.mutable.LinkedHashMap.empty[String, Channel[Array[Byte]]]

  def broadCastMessageToFileRoom(bucket: Bucket, message: FileAction, bytes: Option[Array[Byte]], sender: BSONObjectID)(teams: Teams) =
    writeMessage(message, bytes) match {
      case Some(bytesToSend) =>
        channelPerUser.synchronized {

          teams.collection.find(BSONDocument(
            "_id" -> BSONDocument(
              "$in" -> bucket.teams
            )
          )).cursor[Team].collect[Set]().map {
            ts =>
              val us = (ts.foldLeft(Set.empty[BSONObjectID]) {
                (res, t) => (res ++ t.users) + t.owner
              } + bucket.owner) - sender

              us.foreach {
                case id if channelPerUser.get(id.stringify).nonEmpty =>
                  Logger.debug(s"broadcast message: ${id.stringify} - ${message.typeValue} - $bytes")

                  try {
                    channelPerUser(id.stringify).push(bytesToSend)
                  } catch {
                    case e: Exception =>
                      Logger.warn("BucketManager failed to broadcast", e)
                      channelPerUser.remove(id.stringify)
                  }
                case id =>
                  Logger.warn(s"No openned channel for user: '${id.stringify}'")
              }
          }
        }
      case _ =>
    }

  def readMessage(bytes: Array[Byte]): Option[MessageEnvelop] = {
    try {
      val buffer = ByteBuffer.wrap(bytes)
      buffer.order(ByteOrder.BIG_ENDIAN)

      val headerSize = buffer.getInt
      val messageType = buffer.getInt
      val headerBytes = bytes.slice(buffer.position(), buffer.position() + headerSize)

      MessageEnvelop.read(messageType, headerBytes, if (bytes.size > headerSize + 8)
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