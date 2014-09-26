package controllers

import fly.play.aws.PlayConfiguration
import fly.play.aws.auth.AwsCredentials
import fly.play.s3.{BucketFile, S3}
import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models.{Bucket, Buckets, Standard}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.{WebSocket, BodyParsers, Controller}
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
      println(msg)

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

  def syncBucket(id: String) = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Standard) {
    request =>
      case class SyncBucketItem(path: String, md5: String, lastUpdate: DateTime)

      implicit val syncBucketItemsReads: Reads[SyncBucketItem] = (
        (JsPath \ "path").read[String] and
          (JsPath \ "md5").read[String] and
          (JsPath \ "lastUpdate").read[DateTime]
        )(SyncBucketItem.apply _)

      request.body.validate[Seq[SyncBucketItem]].fold(
        errors => {
          Future.successful(BadRequest(JsError.toFlatJson(errors)))
        },
        bucketItems => {
          buckets.find(BSONObjectID(id)).flatMap {
            case Some(bucket) =>
              Future.sequence(bucketItems.map {
                item =>
                  s3Bucket.get(bucket.name + "/" + item.path).map {
                    case BucketFile(name, contentType, content, acl, headers) =>
                      val dtf = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                      DateTime.parse(headers.get("Last-Modified"), dtf)
//                      println(headers)
                      name
                  }
              }).map {
                list => Ok(Json.toJson(list))
              }
            case None => Future(NotFound(s"Couldn't find bucket for id: $id"))
          }
        }
      )

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
  }

}

object BucketController extends BucketController(Buckets(ReactiveMongoPlugin.db)) with AuthConfigImpl
