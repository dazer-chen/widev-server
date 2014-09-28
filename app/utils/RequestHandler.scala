package utils

import java.nio.ByteBuffer
import akka.util.ByteString
import lib.mongo.DuplicateModel
import models.User
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.{Json, Writes}
import redis.ByteStringFormatter


import scala.concurrent.Future

/**
 * Created by gaetansenn on 26/09/14.
 */


// length : size of the chunck
// offset : position in the origin file



sealed trait FileMode
case object ReadMode extends FileMode
case object InsertMode extends FileMode
case object ReplaceMode extends FileMode
case object DeleteMode extends FileMode

case class FileHeader(filename: String,
                      size: Int)


object FileHeader {
  implicit val byteStringFormatter = new ByteStringFormatter[FileHeader] {
    def serialize(data: FileHeader): ByteString = {
      ByteString(data.filename + "|" + data.size)
    }

    def deserialize(bs: ByteString): FileHeader = {
      val r = bs.utf8String.split('|').toList
      FileHeader(r(0), r(1).toInt)
    }
  }
}

case class FileHeaderRequest(filename: String,
                      size: Int,
                      offset: Int,
                      mode: FileMode)

case class File(header: FileHeaderRequest, data : Option[Array[Byte]])


case class RequestHandlerException(message: String) extends Exception(message)


object FileHeaderRequest {

  implicit object FileModeFormat extends Format[FileMode] {
    def writes(filemode: FileMode) : JsValue = {
      JsString("Test")
    }
    def reads(jsvalue: JsValue) : JsResult[FileMode] = {
      jsvalue.as[String] match {
        case "Read" => JsSuccess(ReadMode, JsPath)
        case "Insert" => JsSuccess(InsertMode, JsPath)
        case "Replace" => JsSuccess(ReplaceMode, JsPath)
        case "Delete" => JsSuccess(DeleteMode, JsPath)
        case x => JsError("Unknown Mode")
      }
    }
  }

  implicit val FileHeaderReads: Reads[FileHeaderRequest] = (
    (JsPath \ "filename").read[String] and
      (JsPath \ "size").read[Int] and
      (JsPath \ "offset").read[Int] and
      (JsPath \ "mode").read[FileMode]
    )(FileHeaderRequest.apply _)

  implicit val FileHeaderWrite = new Writes[FileHeaderRequest] {
    def writes(model: FileHeaderRequest) = Json.obj(
      "filename" -> model.filename,
      "size" -> model.size,
      "offset" -> model.offset
    )
  }
}


object RequestHandler {

  val SIZE_INT = 4

  def readFileFromRequest(data: Array[Byte], info : FileHeaderRequest, header_size: Int): Option[Array[Byte]] = {
    try {
      Some(data.slice(SIZE_INT + header_size, info.size))
    } catch {
      case e : Exception => throw RequestHandlerException("%s".format(e.toString))
    }
  }

  def createResponseHeader(response : JsObject) = {
    val json = response.toString()
    ByteBuffer.allocate(4).putInt(json.length).array()++json.getBytes
  }


  def readHeaderSize(data : Array[Byte]): Int = {
    try {
      ByteBuffer.wrap(data.slice(0,4)).getInt()
    } catch {
      case e : Exception => throw RequestHandlerException("%s".format(e.toString))
    }
  }

  def readFileHeader(data: Array[Byte], size: Int): FileHeaderRequest = {
    try {
      val JsonString = new String(data, 4, size)
      val header = Json.parse(JsonString).validate[FileHeaderRequest]

      header.fold(
        errors => {
          throw RequestHandlerException("Wrong Json Format parsing : %s".format(errors))
        },
        header => {
          header
        }
      )
    } catch {
      case e : Exception => throw RequestHandlerException("%s".format(e.toString))
    }
  }

  def read(data : Array[Byte]): Option[File] = {

    try {
      val sizeHeader = readHeaderSize(data)
      val header = readFileHeader(data, sizeHeader)

      header.mode match {
        case InsertMode | ReplaceMode => {
          readFileFromRequest(data, header, sizeHeader) match {
            case Some(data) => Some(File(header, Some(data)))
            case _ => None
          }
        }
        case DeleteMode | ReadMode => Some(File(header, None))
       }
      None
    } catch {
      case requestException : RequestHandlerException => {
        println("error : %s".format(requestException.message))
        None
      }
    }

//    try {
//      //Read size of Header
//      //Read Json Header
//      val JsonHeader = new String(data, 4, sizeHeader)
//      val header = Json.parse(JsonHeader).validate[FileHeader]
//
//      header.fold(
//        errors => {
//          println("Wrong Json Format parsing : %s".format(errors))
//          None
//        },
//        header => {
//          println(header.mode)
//          None
//        }
//      )
//
//    } catch {
//      case index: StringIndexOutOfBoundsException => {
//        println("out of bounds exception")
//        None
//      }
//      case e: Exception => {
//        println("Wrong Header Format")
//        None
//      }
//    }
  }

}
