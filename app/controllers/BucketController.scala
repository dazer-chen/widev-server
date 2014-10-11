package controllers

import fly.play.aws.PlayConfiguration
import fly.play.aws.auth.AwsCredentials
import fly.play.s3._
import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import lib.util.MD5
import models.{Bucket, BucketFileHeader, Buckets, Standard}
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{BodyParsers, Controller, WebSocket}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

class BucketController(buckets: Buckets) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  val s3Bucket = S3(PlayConfiguration("s3.bucket"))(AwsCredentials.fromConfiguration)

  var ws: Option[WebSocket[String, String]] = None

  val (out, channel) = Concurrent.broadcast[String]
  val in = Iteratee.foreach[String] {
    msg =>
      channel push("I received your message: " + msg)
  }

  def socket = WebSocket.using[String] { request =>
    (in, out)
  }

  def getBucket(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      buckets.find(BSONObjectID(id)).map {
        case Some(bucket) => Ok(Json.toJson(bucket))
        case None => NotFound(s"Couldn't find bucket for id: $id")
      }
  }

  def createBucket = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    request =>
      case class createBucket(name: String, owner: String)

      implicit val createBucketReads: Reads[createBucket] = (
        (JsPath \ "name").read[String] and
          (JsPath \ "owner").read[String]
        )(createBucket.apply _)

      val bucket = request.body.validate[createBucket]

      bucket.fold(
        errors => {
          Future(BadRequest(JsError.toFlatJson(errors)))
        },
        bucket => {
          buckets.create(Bucket(bucket.name, BSONObjectID(bucket.owner))).map {
            bucket =>
              Ok(Json.toJson(bucket))
          } recover {
            case err: DuplicateModel =>
              NotAcceptable(s"User already exists.")
          }
        }
      )
    }

  // TODO check if the file is in memory
  def getFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      val filePath = request.getQueryString("file-path")
      if (filePath.isEmpty)
        Future(BadRequest(s"'name' parameter required."))
      else {
        val filePathHash = MD5.hex_digest(filePath.get)
        buckets.find(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            s3Bucket.get(s"${bucket.name}/$filePathHash").map {
              case BucketFile(path, contentType, content, acl, headers) =>
                Ok(content).withHeaders(headers.getOrElse(Seq()).toSeq:_*)
            }
          case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
      }
  }

  def listFiles(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      buckets.find(BSONObjectID(id)).map {
        case Some(bucket) => Ok(Json.toJson(bucket.files.map { f => Json.toJson(f._2) }))
        case None => NotFound(s"Couldn't find bucket for id: $id")
      }
  }

  // TODO check if the file is in memory
  def uploadFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    case request if request.body.asRaw.nonEmpty =>
      val filePath = request.getQueryString("file-path")
      val contentType = request.getQueryString("content-type")

      if (filePath.isEmpty || contentType.isEmpty)
        Future(BadRequest("Missing parameter(s)."))
      else {
        val bsonId = BSONObjectID(id)
        val filePathSum = MD5.hex_digest(filePath.get)
        val fileContentSum = MD5.hex_digest(request.body.asRaw.get.asBytes().get)

        buckets.findBucketInfos(bsonId).flatMap {
          case Some(bucket) =>
            buckets.findFileHeader(bsonId, filePath.get).flatMap {
              case Some(fileHeader) if fileHeader.md5 == fileContentSum =>
                Logger.debug(s"$filePath is not modified, not uploaded thought")
                Future(Ok)
              case res =>
                val fileHeader = res.getOrElse(BucketFileHeader(filePath.get, fileContentSum)).copy(md5 = fileContentSum, updatedAt = DateTime.now)
                buckets.setFileHeader(BSONObjectID(id), fileHeader = fileHeader).flatMap {
                  _ =>
                    Logger.debug(s"Uploading $filePath to bucket ${bucket.name}")
                    s3Bucket.initiateMultipartUpload(BucketFile(bucket.name + "/" + filePathSum, contentType.get)).flatMap {
                      initTicket =>
                        s3Bucket.uploadPart(initTicket, BucketFilePart(1, request.body.asRaw.get.asBytes().get)).flatMap {
                          partTicket =>
                            s3Bucket.completeMultipartUpload(initTicket, Seq(partTicket)).map {
                              _ => Ok
                            }
                        }
                    }
                }
            }
          case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
      }
    case _ =>
      Future(BadRequest("Content-Type must be set to 'application/octet-stream'."))
  }

  def deleteFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      val filePath = request.getQueryString("file-path")
      if (filePath.isEmpty)
        Future(BadRequest(s"'name' parameter required."))
      else
        buckets.findBucketInfos(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            buckets.deleteFileHeader(BSONObjectID(id), filePath.get).flatMap {
              case true =>
                s3Bucket.remove(s"${bucket.name}/${MD5.hex_digest(filePath.get)}").map {
                  _ => Ok
                }
              case false => Future(NotFound(s"Couldn't find file at path '$filePath.get' in bucket '$id'"))
            }
          case None =>
            Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
  }

}

object BucketController extends BucketController(Buckets(ReactiveMongoPlugin.db)) with AuthConfigImpl
