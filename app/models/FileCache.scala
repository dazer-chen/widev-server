package models

import lib.util.MD5
import play.api.libs.iteratee.Concurrent.Channel
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 10/12/14.
 */

sealed class FileCache(
                        val path: String,
                        val contentType: Option[String],
                        val bytes: scala.collection.mutable.ArrayBuffer[Byte],
                        val users: scala.collection.mutable.LinkedHashMap[BSONObjectID, Int]
                        )

object FileCaches {
  private val collection = scala.collection.mutable.LinkedHashMap.empty[String, FileCache]

  def insert(fd: String, userId: BSONObjectID, at: Int, bytes: Array[Byte], contentType: Option[String] = None): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) =>
        file.bytes.insert(at, bytes:_*)
        if (contentType.nonEmpty)
          collection += (fd -> new FileCache(file.path, contentType, file.bytes, file.users))
        true
      case _ => false
    }
  }

  def remove(fd: String, userId: BSONObjectID, at: Int, length: Int): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) =>
        file.bytes.remove(at, length)
        true
      case _ => false
    }
  }

  def replace(fd: String, userId: BSONObjectID, at: Int, bytes: Array[Byte]): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) =>
        file.bytes.remove(at, bytes.length)
        file.bytes.insert(at, bytes:_*)
        true
      case _ => false
    }
  }

  def clear(fd: String, userId: BSONObjectID): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) =>
        file.bytes.clear()
        true
      case _ => false
    }
  }

  def readAll(fd: String, userId: BSONObjectID): Option[Array[Byte]] = synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) => Some(file.bytes.toArray[Byte])
      case _ => None
    }
  }

  def read(fd: String, userId: BSONObjectID, from: Int, length: Int): Option[Array[Byte]] = synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) =>
        Some(file.bytes.slice(from, length).toArray[Byte])
      case _ => None
    }
  }

  def filePath(fd: String): Option[String] = synchronized {
    collection.get(fd) match {
      case Some(file) => Some(file.path)
      case _ => None
    }
  }

  def fileContentType(fd: String): Option[String] = synchronized {
    collection.get(fd) match {
      case Some(file) => file.contentType
      case _ => None
    }
  }

  def isOpen(bucket: Bucket, filePath: String): Boolean = isOpen(MD5.hex_digest(bucket.physicalFilePath(filePath)))

  def isOpen(fd: String): Boolean = collection.synchronized {
    collection.contains(fd)
  }

  def willClose(fd: String, userId: BSONObjectID): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) if file.users.contains(userId) && file.users.size == 1 => true
      case _ => false
    }
  }

  def users(bucket: Bucket, filePath: String): Set[BSONObjectID] =
    users(MD5.hex_digest(bucket.physicalFilePath(filePath)))

  def users(fd: String): Set[BSONObjectID] = collection.synchronized {
    collection.get(fd) match {
      case Some(file) => file.users.map(_._1).toSet
      case _ => Set.empty
    }
  }

  def open(bucket: Bucket, userId: BSONObjectID, filePath: String): String = collection.synchronized {
    val fd = MD5.hex_digest(bucket.physicalFilePath(filePath))

    collection.get(fd) match {
      case Some(file) =>
        file.users.get(userId) match {
          case Some(n) => file.users += (userId -> (n + 1))
          case None => file.users += (userId -> 1)
        }
      case None =>
        val file = new FileCache(
          filePath,
          None,
          scala.collection.mutable.ArrayBuffer.empty,
          scala.collection.mutable.LinkedHashMap(userId -> 1)
        )
        collection += (fd -> file)
    }

    fd
  }

  def close(fd: String, userId: BSONObjectID): Boolean = collection.synchronized {
    collection.get(fd) match {
      case Some(file) =>
        file.users.get(userId) match {
          case Some(n) if n > 1 =>
            file.users += ((userId, n - 1))
            true
          case Some(_) =>
            file.users -= userId
            if (file.users.size == 0)
              collection -= fd
            true
          case None => false
        }
      case None => false
    }
  }

}