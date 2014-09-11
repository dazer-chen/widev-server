package controllers

import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models.{Standard, Bucket, Buckets}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.Controller
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

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

	def createBucket(name: String, owner: String) = AsyncStack(AuthorityKey -> Standard) {
		request =>
			buckets.create(Bucket(name, BSONObjectID(owner))).map {
				bucket => Ok(Json.toJson(bucket))
			} recover {
				case err: DuplicateModel =>
					NotAcceptable(s"Bucket already exists.")
			}
	}
}

object BucketController extends BucketController(Buckets(ReactiveMongoPlugin.db)) with AuthConfigImpl
