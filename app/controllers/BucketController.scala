package controllers

import jp.t2v.lab.play2.auth.AuthElement
import jp.t2v.lab.play2.stackc.RequestWithAttributes
import lib.util.Crypto
import lib.util.Implicits.BSONDateTimeHandler
import managers.{BucketManager, FileManager}
import messages.{DeleteFileAction, MessageEnvelop, AddFileAction}
import models.{Bucket, _}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.{BSONObjectID, BSONDocument}

import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

class BucketController(buckets: Buckets, teams: Teams) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  private def canReadAndEditBucket(id: String)(f: Bucket => Future[Result])(implicit req: RequestWithAttributes[_]) = {
    val user = loggedIn

    buckets.find(BSONObjectID(id)).flatMap {
      case Some(bucket) =>
        buckets.userCanReadAndEdit(BSONObjectID(id), user._id).flatMap {
          case true =>
            f(bucket)
          case _ =>
            Future(Unauthorized(s"Permission denied."))
        }
      case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
    }
  }

  def getBucket(id: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      canReadAndEditBucket(id) {
        bucket =>
          Future(Ok(Json.toJson(bucket)))
      }
  }

  def getBuckets = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn

      val name = request.getQueryString("name")
      if (name.isEmpty)
        buckets.findByUser(user._id).map(
          bs =>
            Ok(Json.toJson(bs))
        )
      else {
        buckets.find(name.get).flatMap {
          case Some(bucket) =>
            buckets.userCanReadAndEdit(bucket._id, loggedIn._id).map {
              case true =>
                Ok(Json.toJson(bucket))
              case _ =>
                Unauthorized(s"Permission denied.")
            }
          case _ =>
            Future(NotFound(s"Couldn't find bucket for name: '${name.get}'"))
        }
      }
  }

  def updateTeams(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val teams = (request.body \ "teams").validate[Seq[String]]

      teams.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        teams => {
          canReadAndEditBucket(id) {
            _ =>
              buckets.updateTeams(BSONObjectID(id), teams.map(BSONObjectID(_)).toSet).map {
                case true => Ok
                case false => BadRequest(s"Unable to update teams")
              }
          }
        }
      )
  }

  def addTeam(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      canReadAndEditBucket(id) {
        bucket =>
          val team = (request.body \ "team").validate[String]

          team.fold(
            errors => {
              Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
            },
            team => {
              buckets.addTeam(BSONObjectID.apply(id), BSONObjectID.apply(team)).map {
                case true => Ok
                case _ => BadRequest(s"Unable to add the team $team")
              }
            }
          )
      }
  }

  def removeTeam(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      buckets.find(BSONObjectID.apply(id)).flatMap {
        case Some(bucket) => {
          val team = (request.body \ "team").validate[String]

          team.fold(
            errors => {
              Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
            },
            team => {
              canReadAndEditBucket(id) {
                bucket =>
                  buckets.removeTeam(bucket._id, BSONObjectID.apply(team)).map {
                    case true => Ok
                    case _ => BadRequest(s"Unable to remove the team $team")
                  }
              }
            }
          )
        }
        case None => Future(NotFound(s"Bucket $id not found"))
      }
  }

  def createBucket = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn

      import models.Bucket._

      val bucket = request.body.validate[CreateBucket]

      bucket.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        bucket => {
          val toInsert = Bucket(
            name = bucket.informations.name,
            owner = user._id,
            version = bucket.informations.version,
            files = bucket.files,
            navigator = bucket.navigator,
            project = bucket.project,
            targets = bucket.targets
          )
          buckets.collection.insert(toInsert).map {
            _ =>
              Ok(Json.toJson(toInsert))
          }.recover {
            case e: Throwable =>
              Logger.error("Couldn't create bucket", e)
              NotAcceptable(s"Bucket already exists.")
          }
        })
  }

  def updateBucket(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn

      import models.Bucket._

      val bucket = request.body.validate[CreateBucket]

      bucket.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        bucket => {
          buckets.collection.update(BSONDocument("_id" -> BSONObjectID(id)),
            BSONDocument(
              "$set" -> BSONDocument(
                "name" -> bucket.informations.name,
                "version" -> bucket.informations.version,
                "files" -> bucket.files,
                "navigator" -> bucket.navigator,
                "project" -> bucket.project,
                "targets" -> bucket.targets,
                "update" -> BSONDateTimeHandler.write(DateTime.now(DateTimeZone.UTC))
              )
            )
          ).flatMap {
            _ =>
              buckets.find(BSONObjectID(id)).map {
                case Some(bucket) =>
                  Ok(Json.toJson(bucket))
                case None =>
                  InternalServerError
              }
          }.recover {
            case e: Throwable =>
              Logger.error("Couldn't create bucket", e)
              NotAcceptable(s"Bucket already exists.")
          }
        })
  }

  def getFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val filePath = request.getQueryString("file-path")

      if (filePath.isEmpty)
        Future(BadRequest(s"'file-path' parameter required."))
      else
        canReadAndEditBucket(id) {
          bucket =>
            buckets.findFileInBucket(bucket._id, filePath.get).flatMap {
              case Some(file) =>
                FileManager.readAll(bucket, filePath.get).map {
                  case Some(content) => Ok(content)
                  case None => InternalServerError
                }
              case None =>
                Future(NotFound(s"File '$filePath' not found"))
            }
        }
  }

  def listFiles(id: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>

      canReadAndEditBucket(id) {
        bucket =>
          buckets.userCanReadAndEdit(BSONObjectID(id), loggedIn._id).map {
            case true =>
              Ok(Json.toJson(bucket.files.map(_.path)))
            case _ =>
              Unauthorized(s"You cannot access to this bucket.")
          }
      }
  }

  def removeFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    implicit request =>
      val user = loggedIn
      val filePath = request.getQueryString("file-path")

      if (filePath.isEmpty)
        Future(BadRequest(s"'file-path' parameter required."))
      else
        canReadAndEditBucket(id) {
          bucket =>
            buckets.removeFileFromBucket(bucket._id, filePath.get).map {
              case true =>
                FileManager.delete(bucket, filePath.get)
                Ok // TODO broadcast to the connected team members
              case _ =>
                NotFound(s"Couldn't find file '$filePath'")
            }
        }
  }

  // TODO save in a local file
  def uploadFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    case request if request.body.asRaw.nonEmpty =>
      val filePath = request.getQueryString("file-path")
      val encoding = request.getQueryString("encoding")

      if (filePath.isEmpty || encoding.isEmpty)
        Future(BadRequest("Missing parameter(s)."))
      else
        canReadAndEditBucket(id)({
          bucket =>
            buckets.addFileToBucket(bucket._id, filePath.get, encoding.get).flatMap {
              case true =>
                FileManager.delete(bucket, filePath.get)
                FileManager.replace(bucket, filePath.get, 0, request.body.asRaw.get.asBytes().get).map {
                  case true =>
                    Ok
                  case _ =>
                    InternalServerError(s"Couldn't create file s'$filePath'")
                }
              case _ =>
                Future(InternalServerError(s"Couldn't create file s'$filePath'"))
            }
        })(request)
    case _ =>
      Future(BadRequest("Content-Type must be set to 'application/octet-stream'."))
  }

  /* -------- web socket handling -------- */

  def socket = WebSocket.tryAccept[Array[Byte]] {
    implicit request =>
      Future {
        val tokenValue = request.getQueryString("sessionToken") match {
          case Some(token) => Crypto.verifyHmac(token)
          case _ => None
        }
        tokenValue match {
          case Some(token) =>
            idContainer.get(token) match {
              case Some(userId) =>
                val (out, channel) = Concurrent.broadcast[Array[Byte]]
                val in = Iteratee.foreach[Array[Byte]](receiveMessage)

                BucketManager.channelPerUser.synchronized {
                  BucketManager.channelPerUser += (userId -> channel)
                }

                Right((in, out))
              case None => Left(NotFound(s"Session not found"))
            }
          case None => Left(NotAcceptable(s"Received an invalid message"))
        }
      }
  }

  def receiveMessage(bytes: Array[Byte]): Unit =
    BucketManager.readMessage(bytes) match {
      case Some(envelop) =>
        Logger.debug(s"received: ${envelop.message}")

        val tokenValue = envelop.message.sessionToken match {
          case Some(token) => Crypto.verifyHmac(token)
          case _ => None
        }

        def action(envelop: MessageEnvelop, bucket: Bucket, userId: String) =
          envelop.message.action(bucket, envelop.bytes, (messageToBroadcast, bytes) => {
            buckets.markAsUpdated(bucket._id)
            BucketManager.broadCastMessageToFileRoom(bucket, messageToBroadcast, bytes, BSONObjectID(userId))(teams)
          })(buckets)

        tokenValue match {
          case Some(token) =>
            idContainer.get(token) match {
              case Some(userId) =>
                // NOTE this might be a little heavy !?
                val message = envelop.message
                buckets.findBucketEnsuringFileExists(BSONObjectID(message.bucketId), message.filePath).map {
                  case Some(bucket) =>
                    action(envelop, bucket, userId)
                  case None if message.typeValue == AddFileAction.typeValue || message.typeValue == DeleteFileAction.typeValue =>
                    buckets.find(BSONObjectID(message.bucketId)).map {
                      case Some(bucket) =>
                        action(envelop, bucket, userId)
                      case None =>
                        Logger.warn(s"Bucket '${message.bucketId}' not found.")
                    }
                  case None =>
                    Logger.warn(s"Bucket '${message.bucketId}' with file '${envelop.message.filePath}' not found.")
                }
              case None =>
                Logger.warn(s"Unknown session token: '${envelop.message.sessionToken.get}'.")
            }
          case None =>
            Logger.warn(s"Unhandled message")
        }
      case None => Logger.debug(s"Invalid message")
    }

}

object BucketController extends BucketController(
  Buckets(ReactiveMongoPlugin.db),
  Teams(ReactiveMongoPlugin.db)
) with AuthConfigImpl
