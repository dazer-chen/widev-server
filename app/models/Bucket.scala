package models

import lib.mongo.{Collection, SuperCollection}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

case class Bucket(
											name: String,
											owner: BSONObjectID,
	                    _id: BSONObjectID = BSONObjectID.generate
	                  )

object Bucket {
	implicit val handler = Macros.handler[Bucket]

	implicit val BucketWrites = new Writes[Bucket] {
		def writes(model: Bucket) = Json.obj(
			"_id" -> model._id.stringify,
			"name" -> model.name,
			"owner" -> model.owner.stringify
		)
	}

	def generate = Bucket(
		name = BSONObjectID.generate.stringify,
		owner = BSONObjectID.generate
	)
}

case class Buckets(db: DefaultDB) extends Collection[Bucket] {
	val collection = db.collection[BSONCollection]("buckets")

	override def generate: Bucket = Bucket.generate

	override def relations: Seq[SuperCollection] = Seq.empty

	def find(name: String, owner: BSONObjectID): Future[Option[Bucket]] =
		collection.find(BSONDocument("name" -> name, "owner" -> owner)).one[Bucket]

	def deleteByName(name: String): Future[LastError] =
		collection.remove(BSONDocument("name" -> name))
}
