package models

import lib.mongo.{Collection, SuperCollection}
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}
import reactivemongo.core.commands.LastError

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by thomastosoni on 8/31/14.
 */

case class Workspace(
											name: String,
											owner: BSONObjectID,
	                    _id: BSONObjectID = BSONObjectID.generate
	                  )

object Workspace {
	implicit val handler = Macros.handler[Workspace]

	implicit val WorkspaceWrites = new Writes[Workspace] {
		def writes(model: Workspace) = Json.obj(
			"_id" -> model._id.stringify,
			"name" -> model.name,
			"owner" -> model.owner.stringify
		)
	}

	def generate = Workspace(
		name = BSONObjectID.generate.stringify,
		owner = BSONObjectID.generate
	)
}

case class Workspaces(db: DefaultDB) extends Collection[Workspace] {
	val collection = db.collection[BSONCollection]("workspaces")

	override def generate: Workspace = Workspace.generate

	override def relations: Seq[SuperCollection] = Seq.empty

//	def find(name: String, owner: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[Workspace]] =
//		collection.find(BSONDocument("name" -> name, "owner" -> owner)).one[Workspace]
//
//	def deleteByName(name: String): Future[LastError] =
//		collection.remove(BSONDocument("name" -> name))
}
