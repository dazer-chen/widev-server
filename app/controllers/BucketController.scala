package controllers

import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models.{User, Standard, Bucket, Buckets}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, JsPath, Reads, Json}
import play.api.mvc.{Action, BodyParsers, Controller}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID
import play.api.libs.functional.syntax._

import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

class BucketController(buckets: Buckets) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  def getBucket(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      buckets.find(BSONObjectID(id)).map {
        case Some(bucket) => Ok(Json.toJson(bucket))
        case None => NotFound(s"Couldn't find workspace for id: $id")
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
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        bucket => {
          buckets.create(Bucket(bucket.name, BSONObjectID(bucket.owner))).map {
            bucket => Ok(Json.toJson(bucket))
          } recover {
            case err: DuplicateModel =>
              NotAcceptable(s"User already exists.")
          }
        }
      )
    }
}

object BucketController extends BucketController(Buckets(ReactiveMongoPlugin.db)) with AuthConfigImpl
