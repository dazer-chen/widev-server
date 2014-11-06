package models

import controllers.AuthConfigImpl
import lib.mongo.{SuperCollection, Collection}
import lib.util.BearerTokenGenerator
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by gaetansenn on 17/10/14.
 */

case class Team(
									name: String,
									owner: BSONObjectID,
                  users: Set[BSONObjectID],
                  createdAt: DateTime = DateTime.now,
                  updatedAt: DateTime = DateTime.now,
                  _id: BSONObjectID = BSONObjectID.generate
                )

object Team {
	import lib.util.Implicits._

  implicit val handler = Macros.handler[Team]

	implicit val TeamWrites = new Writes[Team] {
		def writes(model: Team) = Json.obj(
			"name" -> model.name,
			"owner" -> model.owner.stringify,
			"users" -> model.users.map(_.stringify),
			"createdAt" -> model.createdAt,
			"updatedAt" -> model.updatedAt,
			"_id" -> model._id.stringify
		)
	}

  def generate: Team = Team(
    name = "team",
    owner = BSONObjectID.generate,
    users = Set((0 to 10).map(_ => BSONObjectID.generate):_*)
  )
}

case class Teams(db: DefaultDB) extends Collection[Team] with AuthConfigImpl {

  val collection = db.collection[BSONCollection]("team")

  def relations: Seq[SuperCollection] = Seq.empty

  def generate: Team = Team.generate

  def addUser(teamId: BSONObjectID, userId: BSONObjectID): Future[Boolean] = {
    collection.update(BSONDocument("_id" -> teamId), BSONDocument(
      "$addToSet" -> BSONDocument(
        "users" ->
          userId
      )
    )).map {
      case (updated) => {
        true
      }
      case _ => false
    }
  }

  def removeUser(teamId: BSONObjectID, userId: BSONObjectID): Future[Boolean] = {
    collection.update(BSONDocument("_id" -> teamId), BSONDocument(
      "$pull" -> BSONDocument(
        "users" ->
          userId
      )
    )).map {
      case (updated) => {
        true
      }
      case _ => false
    }
  }

	def find(name: String, owner: BSONObjectID): Future[Option[Team]] =
		collection.find(BSONDocument("name" -> name, "owner" -> owner)).one[Team]

	def findByOwner(owner: BSONObjectID) =
		collection.find(BSONDocument("owner" -> owner)).cursor[Team].collect[List]()

	override def save(model: Team): Future[Team] =
		super.save(model.copy(createdAt = DateTime.now()))

	override def update(model: Team): Future[Boolean] =
		super.update(model.copy(updatedAt = DateTime.now()))
}