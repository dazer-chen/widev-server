package controllers

import fly.play.aws.PlayConfiguration
import fly.play.aws.auth.AwsCredentials
import fly.play.s3._
import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models.{Bucket, Buckets, Standard}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.Play.current
import play.api.http.MimeTypes
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

      implicit val createUserReads: Reads[createBucket] = (
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

  def getFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      val name = request.getQueryString("file-path")
      if (name.isEmpty)
        Future(BadRequest(s"'name' parameter required."))
      else
        buckets.find(BSONObjectID(id)).flatMap {
          case Some(bucket) =>
            s3Bucket.get(s"${bucket.name}/${name.get}").map {
              case BucketFile(path, contentType, content, acl, headers) =>
                Ok(content).withHeaders(headers.getOrElse(Seq()).toSeq:_*)
            }
          case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
  }

  def listFiles(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      buckets.find(BSONObjectID(id)).flatMap {
        case Some(bucket) =>
          implicit val FileWrites = new Writes[BucketItem] {
            override def writes(o: BucketItem): JsValue = Json.obj(
              "path" -> o.name.substring(bucket.name.length + 1),
              "isVirtual" -> o.isVirtual
            )
          }
          s3Bucket.list(s"${bucket.name}/").map {
            items =>
              Ok(Json.toJson(items.filterNot {
              case BucketItem(name, _) =>
                name.endsWith("/")
            }))
          }
        case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
      }
  }

  def uploadFile(id: String) = AsyncStack(AuthorityKey -> Standard) {
    case request if request.body.asRaw.nonEmpty =>
      val name = request.getQueryString("file-path")
      val contentType = request.getQueryString("content-type")

      if (name.isEmpty || contentType.isEmpty)
        Future(BadRequest("Missing parameter(s)."))
      else
      buckets.find(BSONObjectID(id)).flatMap {
        case Some(bucket) =>
          s3Bucket.initiateMultipartUpload(BucketFile(bucket.name + "/" + name.get, contentType.get)).flatMap {
              initTicket =>
                s3Bucket.uploadPart(initTicket, BucketFilePart(1, request.body.asRaw.get.asBytes().get)).flatMap {
                  partTicket =>
                    s3Bucket.completeMultipartUpload(initTicket, Seq(partTicket)).map {
                      _ => Ok
                    }
                }
          }
          case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
        }
    case _ =>
      Future(BadRequest("Content-Type must be set to 'application/octet-stream'."))
  }

//  def syncBucket(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
//    request =>
//      case class SyncBucketItem(path: String, md5: String, lastUpdate: DateTime)
//
//      implicit val syncBucketItemsReads: Reads[SyncBucketItem] = (
//        (JsPath \ "path").read[String] and
//          (JsPath \ "md5").read[String] and
//          (JsPath \ "lastUpdate").read[DateTime]
//        )(SyncBucketItem.apply _)
//
//      request.body.validate[Seq[SyncBucketItem]].fold(
//        errors => {
//          Future.successful(BadRequest(JsError.toFlatJson(errors)))
//        },
//        bucketItems => {
//          buckets.find(BSONObjectID(id)).flatMap {
//            case Some(bucket) =>
//              Future.sequence(bucketItems.map {
//                item =>
//                  s3Bucket.get(bucket.name + "/" + item.path).map {
//                    case BucketFile(name, contentType, content, acl, headers) =>
//                      val dtf = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
//                      val date = DateTime.parse(headers.get("Last-Modified"), dtf)
//                      date
//                  }
//              }).map {
//                list => Ok(Json.toJson(list))
//              }
//            case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
//          }
//        }
//      )

//      buckets.find(BSONObjectID(id)).flatMap {
//        case Some(bucket) =>
//          s3Bucket.list(s"${bucket.name}/").map {
//            it =>
//              Ok(Json.toJson(it.map {
//                item => item.name
//              }))
//          }
//        case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
//      }
//  }

}

object BucketController extends BucketController(Buckets(ReactiveMongoPlugin.db)) with AuthConfigImpl
