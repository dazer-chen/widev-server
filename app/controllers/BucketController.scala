package controllers

import java.nio.ByteBuffer

import fly.play.aws.PlayConfiguration
import fly.play.aws.auth.AwsCredentials
import fly.play.s3._
import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import lib.util.MD5
import managers.BucketManager
import messages._
import models.{Bucket, _}
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.mvc.{BodyParsers, Controller, WebSocket}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID
import play.api.libs.json._
import play.api.libs.functional.syntax._


import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

class BucketController(buckets: Buckets, s3Bucket: fly.play.s3.Bucket) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  private[controllers] def uploadFileToS3(bucket: Bucket, filePath: String, contentType: String, bytes: Array[Byte]): Future[Unit] =
    s3Bucket.initiateMultipartUpload(BucketFile(bucket.physicalFilePath(filePath), contentType)).flatMap {
      initTicket =>
        s3Bucket.uploadPart(initTicket, BucketFilePart(1, bytes)).flatMap {
          partTicket =>
            s3Bucket.completeMultipartUpload(initTicket, Seq(partTicket))
        }
    }

  def getBucket(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      buckets.find(BSONObjectID(id)).map {
        case Some(bucket) => Ok(Json.toJson(bucket))
        case None => NotFound(s"Couldn't find bucket for id: $id")
      }
  }

  def getBuckets = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn
      buckets.findByOwner(user._id).map(bs => Ok(Json.toJson(bs)))
  }

  def updateTeam = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn

      case class updateTeam(id: String, team: Set[String])

      implicit val createTeamReads: Reads[updateTeam] = (
        (JsPath \ "id").read[String] and
        (JsPath \ "teams").read[Set[String]]
        )(updateTeam.apply _)

      val team = request.body.validate[updateTeam]

      team.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        team => {
          val user = loggedIn

          buckets.updateTeam(BSONObjectID(team.id), team.team.map(BSONObjectID(_))).map {
            case true => Ok("")
            case false => BadRequest("")
          }
        }
      )
  }

  def createBucket = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val bucketName = request.getQueryString("name")
      val user = loggedIn

      if (bucketName.isEmpty) {
        Future(BadRequest(s"'name' parameter required."))
      }
      else {
        buckets.create(Bucket(bucketName.get, user._id)).map {
          bucket =>
            Ok(Json.toJson(bucket))
        } recover {
          case err: DuplicateModel =>
            NotAcceptable(s"User already exists.")
        }
      }
    }

  // TODO check if the file is in memory
  def getFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      val filePath = request.getQueryString("file-path")
      if (filePath.isEmpty)
        Future(BadRequest(s"'name' parameter required."))
      else
        buckets.find(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            s3Bucket.get(bucket.physicalFilePath(filePath.get)).map {
              case BucketFile(path, contentType, content, acl, headers) =>
                Ok(content).withHeaders(headers.getOrElse(Seq()).toSeq:_*)
            }
          case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
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
      val user = loggedIn(request)

      if (filePath.isEmpty || contentType.isEmpty)
        Future(BadRequest("Missing parameter(s)."))
      else {
        val bsonId = BSONObjectID(id)
        val fileContentSum = MD5.hex_digest(request.body.asRaw.get.asBytes().get)

        buckets.findBucketInfos(bsonId).flatMap {
          case Some(bucket) =>
            val fd = FileCaches.open(bucket, user._id, filePath.get)
            val cacheFileContentSum = MD5.hex_digest(FileCaches.readAll(fd, user._id).get)

            if (fileContentSum != cacheFileContentSum) {
              FileCaches.clear(fd, user._id)
              FileCaches.insert(fd, user._id, 0, request.body.asRaw.get.asBytes().get)
              FileCaches.willClose(fd, user._id) match {
                case true => // we need to upload the file so we don't loose data
                  FileCaches.close(fd, user._id)
                  buckets.findFileHeader(bsonId, filePath.get).flatMap {
                    case Some(fileHeader) if fileHeader.md5 == fileContentSum =>
                      Logger.debug(s"$filePath is not modified, not uploaded thought")
                      Future(Ok)
                    case res =>
                      val fileHeader = res.getOrElse(BucketFileHeader(filePath.get, fileContentSum)).copy(md5 = fileContentSum, updatedAt = DateTime.now)
                      buckets.setFileHeader(BSONObjectID(id), fileHeader = fileHeader).flatMap {
                        _ =>
                          Logger.debug(s"Uploading $filePath to bucket ${bucket.name}")
                          uploadFileToS3(bucket, filePath.get, contentType.get, request.body.asRaw.get.asBytes().get).map {
                            _ => Ok
                          }
                      }
                  }
                case false => Future(Ok) // will stay in cache anyway
              }
            }
            else Future(Ok)
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
                s3Bucket.remove(bucket.physicalFilePath(filePath.get)).map {
                  _ => Ok
                }
              case false => Future(NotFound(s"Couldn't find file at path '$filePath.get' in bucket '$id'"))
            }
          case None =>
            Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
  }

  def openFileCache(id: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn
      val filePath = request.getQueryString("file-path")

      if (filePath.isEmpty)
        Future(BadRequest(s"'name' parameter required."))
      else {
        buckets.findBucketInfos(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            val alreadyOpen = FileCaches.isOpen(bucket, filePath.get)
            val fd = FileCaches.open(bucket, user._id, filePath.get)

            if (!alreadyOpen)
              s3Bucket.get(bucket.physicalFilePath(filePath.get)).map {
                case BucketFile(_, contentType, content, acl, headers) =>
                  FileCaches.insert(fd, user._id, 0, content, Some(contentType))
                  Ok(fd)
              }
            else Future(Ok(fd))

          case None =>
            Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
      }
  }

  def closeFileCache(id: String, fd: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn
      val filePath = FileCaches.filePath(fd)
      val contentType = FileCaches.fileContentType(fd).getOrElse("application/octet-stream")

      if (FileCaches.willClose(fd, user._id))
        buckets.findBucketInfos(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            uploadFileToS3(bucket, filePath.get, contentType, FileCaches.readAll(fd, user._id).get).map {
              _ =>
                FileCaches.close(fd, user._id)
                Ok
            }
          case None =>
            Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
      else
        Future(FileCaches.close(fd, user._id) match {
          case true => Ok
          case false => NotFound(s"Couldn't find any fd '$fd' in bucket '$id' for your session")
        })
  }

  /* -------- web socket handling -------- */

  def socket =
    WebSocket.using[Array[Byte]] {
      implicit request =>
        val (out, channel) = Concurrent.broadcast[Array[Byte]]
        val in = Iteratee.foreach[Array[Byte]](receiveMessage)

        StackAction(AuthorityKey -> Standard) {
          implicit request =>
            val user = loggedIn

            BucketManager.channelPerUser.synchronized {
              BucketManager.channelPerUser += (user._id.stringify -> channel)
            }

            Ok
        }

        (in, out)
    }

  def receiveMessage(bytes: Array[Byte]): Unit =
    BucketManager.readMessage(bytes) match {
      case Some(envelop) =>
        if (envelop.message.sessionToken.nonEmpty)
          idContainer.get(envelop.message.sessionToken.get) match {
            case Some(userId) =>
              envelop.message.action(BSONObjectID(userId), envelop.bytes, (message, bytes) =>
                BucketManager.broadCastMessageToFileRoom(message, bytes, BSONObjectID(userId))
              )
            case None =>
              Logger.warn(s"Unknown session token: '${envelop.message.sessionToken}'.")
          }
        else
          Logger.warn(s"Unhandled message")
      case None => Logger.debug(s"Received an invalid message")
    }

}

object BucketController extends BucketController(
  Buckets(ReactiveMongoPlugin.db),
  S3(PlayConfiguration("s3.bucket"))(AwsCredentials.fromConfiguration)
) with AuthConfigImpl
