package managers

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{Files, Paths, StandardOpenOption}
import play.api.libs.concurrent.Execution.Implicits._

import models.Bucket
import play.api.Logger

import scala.concurrent.{Future, Promise}

/**
 * Created by trupin on 1/4/15.
 */
object FileManager {

  private val bufSize = 4096

  private def physicalPath(bucket: Bucket, filePath: String) =
    Paths.get("./data/users/", bucket.owner.stringify, "buckets", bucket._id.stringify, "files", filePath)

  private case class WriterHandler(promise: Promise[Unit]) extends CompletionHandler[Integer, Object] {
    def failed(exc: Throwable, attachment: Object) = {
      promise.failure(exc)
    }

    def completed(result: Integer, attachment: Object) = {
      promise.success(Unit)
    }
  }

  private def write(bytes: Array[Byte], position: Int)(implicit channel: AsynchronousFileChannel): Future[Unit] = {
    val promise = Promise[Unit]()

    channel.write(ByteBuffer.wrap(bytes), position, "write to file", WriterHandler(promise))

    promise.future
  }

  private case class ReaderHandler(promise: Promise[Array[Byte]], buffer: ByteBuffer) extends CompletionHandler[Integer, Object] {
    def failed(exc: Throwable, attachment: Object) = {
      promise.failure(exc)
    }

    def completed(result: Integer, attachment: Object) = {
      promise.success(buffer.array())
    }
  }

  private def read(position: Int, length: Int)(implicit channel: AsynchronousFileChannel): Future[Array[Byte]] = {
    val promise = Promise[Array[Byte]]()

    val byteBuffer = ByteBuffer.allocate(length)

    channel.read(byteBuffer, position, "read from file", ReaderHandler(promise, byteBuffer))

    promise.future
  }

  def insert(bucket: Bucket, filePath: String, position: Int, bytes: Array[Byte]): Future[Boolean] = {
    val ppath = physicalPath(bucket, filePath)

    try {
      implicit val channel = AsynchronousFileChannel.open(ppath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
      channel.force(true)

      read(position, channel.size().toInt).flatMap {
        bytesToMove =>
          write(bytesToMove ++ bytes, position).map {
            _ =>
              channel.close()
              true
          }
      }.recover {
        case e: IOException =>
          Logger.error(e.getMessage)
          false
      }
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        Future(false)
    }
  }

  def delete(bucket: Bucket, filePath: String, position: Int, length: Int): Future[Boolean] = {
    val ppath = physicalPath(bucket, filePath)

    try {
      implicit val channel = AsynchronousFileChannel.open(ppath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
      channel.force(true)

      val originalSize = channel.size().toInt
      read(position + length, originalSize).flatMap {
        bytesToMove =>
          write(bytesToMove, position).map {
            _ =>
              channel.truncate(originalSize - length)
              channel.close()
              true
          }
      }.recover {
        case e: IOException =>
          Logger.error(e.getMessage)
          false
      }
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        Future(false)
    }
  }

  def replace(bucket: Bucket, filePath: String, position: Int, bytes: Array[Byte]): Future[Boolean] = {
    val ppath = physicalPath(bucket, filePath)

    try {
      implicit val channel = AsynchronousFileChannel.open(ppath, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
      channel.force(true)

      write(bytes, position).map {
        _ =>
          channel.close()
          true
      }.recover {
        case e: IOException =>
          Logger.error(e.getMessage)
          false
      }
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        Future(false)
    }
  }

  def readAll(bucket: Bucket, filePath: String): Future[Option[Array[Byte]]] = {
    val ppath = physicalPath(bucket, filePath)

    try {
      implicit val channel = AsynchronousFileChannel.open(ppath, StandardOpenOption.READ)

      read(0, channel.size().toInt).map {
        bytes =>
          channel.close()
          Some(bytes)
      }.recover {
        case e: IOException =>
          Logger.error(e.getMessage)
          None
      }
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        Future(None)
    }
  }

  def delete(bucket: Bucket, filePath: String): Boolean = {
    val ppath = physicalPath(bucket, filePath)
    Files.deleteIfExists(ppath)
  }

  def move(bucket: Bucket, fromPath: String, toPath: String): Boolean = {
    val fromPPath = physicalPath(bucket, fromPath)
    val toPPath = physicalPath(bucket, toPath)
    try {
      Files.move(fromPPath, toPPath)
      true
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        false
    }
  }

  def copy(bucket: Bucket, fromPath: String, toPath: String): Boolean = {
    val fromPPath = physicalPath(bucket, fromPath)
    val toPPath = physicalPath(bucket, toPath)

    try {
      Files.copy(fromPPath, toPPath)
      true
    } catch {
      case e: IOException =>
        Logger.error(e.getMessage)
        false
    }
  }
}
