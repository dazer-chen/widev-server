package models

import lib.mongo.Collection
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.LastError

import scala.concurrent.Future
import lib.util.Implicits._

case class File(
                 path: String,
                 encoding: String,
                 id: Int = 0
                 )

object File {
  implicit val handler = Macros.handler[File]

  implicit val fileWrites = new Writes[File] {
    override def writes(o: File): JsValue = Json.obj(
      "path" -> o.path,
      "id" -> o.id,
      "encoding" -> o.encoding
    )
  }
}

case class Informations(

                         )

object Informations {
  implicit val handler = Macros.handler[File]

  implicit val informationWrites = new Writes[Informations] {
    override def writes(o: Informations): JsValue = Json.obj(

    )
  }
}

case class Bucket(
                   name: String,
                   owner: BSONObjectID,
                   version: Int = 0,
                   teams: Set[BSONObjectID] = Set.empty,
                   creation: DateTime = DateTime.now(),
                   update: DateTime = DateTime.now(),
                   files: Array[File] = Array.empty,
                   _id: BSONObjectID = BSONObjectID.generate
                   )

object Bucket {
  implicit val handler = Macros.handler[Bucket]

  implicit val BucketWrites = new Writes[Bucket] {
    override def writes(o: Bucket) = Json.obj(
      "_id" -> o._id.stringify,
      "files" -> o.files,
      "informations" -> Json.obj(
        "creation" -> o.creation,
        "update" -> o.update,
        "name" -> o.name,
        "version" -> o.version
      ),
      "navigator" -> Json.arr( // TODO generate a valid navigator handling directories
        Json.obj(
          "children" -> Json.arr(0),
          "id" -> 1,
          "kind" -> 0,
          "name" -> "root",
          "path" -> ""
        )
      )
    )
  }

	def generate = ???
}

case class Buckets(db: DefaultDB) extends Collection[Bucket] {
	val collection = db.collection[BSONCollection]("buckets")

	override def generate: Bucket = Bucket.generate

  val factory = Factory(db)

  override def relations = Seq(factory.teams)

  def find(name: String, owner: BSONObjectID): Future[Option[Bucket]] =
		collection.find(
      BSONDocument("name" -> name, "owner" -> owner)
    ).one[Bucket]

	def deleteByName(name: String): Future[LastError] =
		collection.remove(BSONDocument("name" -> name))

  def findByOwner(owner: BSONObjectID) = {
    collection.find(BSONDocument("owner" -> owner)).cursor[Bucket].collect[List]()
  }

  def findByUser(user: BSONObjectID) = {
    findByOwner(user).flatMap {
      ownedBuckets =>
        factory.teams.findByUser(user).flatMap {
          otherTeams =>
            collection.find(BSONDocument("teams" -> BSONDocument("$in" -> otherTeams.map(_._id)))).cursor[Bucket].collect[Set]().map(_ ++ ownedBuckets)
        }
    }
  }

  def userCanReadAndEdit(bucket: BSONObjectID, user: BSONObjectID) = {
    find(bucket).flatMap {
      case Some(bucket) if bucket.owner == user => Future(true)
      case Some(bucket) => factory.teams.isUserInOneTeam(bucket.teams, user)
      case _ => Future(false)
    }
  }

  def addTeam(bucketId: BSONObjectID, teamId: BSONObjectID): Future[Boolean] = {
    collection.update(BSONDocument("_id" -> bucketId), BSONDocument(
      "$addToSet" -> BSONDocument(
        "teams" ->
          teamId
      )
    )).map { _ => true }
  }

  def removeTeam(bucketId: BSONObjectID, teamId: BSONObjectID): Future[Boolean] = {
    collection.update(BSONDocument("_id" -> bucketId), BSONDocument(
      "$pull" -> BSONDocument(
        "teams" ->
          teamId
      )
    )).map { _ => true }
  }

  def findFileInBucket(bucketId: BSONObjectID, path: String): Future[Option[File]] = {
    collection.find(BSONDocument("_id" -> bucketId, "files.path" -> path), BSONDocument("files.$" -> 1)).one[BSONDocument].map {
      case Some(document) =>
        document.getAs[File]("files")
      case _ => None
    }
  }

  def findBucketEnsuringFileExists(bucketId: BSONObjectID, path: String): Future[Option[Bucket]] = {
    collection.find(BSONDocument("_id" -> bucketId, "files.path" -> path)).one[Bucket]
  }

  def removeFileFromBucket(bucketId: BSONObjectID, path: String): Future[Boolean] = {
    collection.update(BSONDocument("_id" -> bucketId, "files.path" -> path), BSONDocument(
      "$pull" -> BSONDocument(
        "files" -> BSONDocument(
          "path" -> path
        )
      )
    )).map {
      case res if res.updated > 0 => true
      case _ => false
    }
  }

  def addFileToBucket(bucketId: BSONObjectID, file: File): Future[Boolean] = {
    def update = collection.update(BSONDocument("_id" -> bucketId), BSONDocument(
      "$push" -> BSONDocument(
        "files" -> file
      )
    )).map(_ => true)

    findFileInBucket(bucketId, file.path).flatMap {
      case Some(_) => removeFileFromBucket(bucketId, file.path).flatMap(_ => update)
      case None => update
    }
  }

  def updateTeams(id: BSONObjectID, team: Set[BSONObjectID]): Future[Boolean] =
    collection.update(BSONDocument("_id" -> id), BSONDocument(
      "$set" -> BSONDocument(
        "teams" -> team
      )
    )).map {
      case res if res.updated > 0 => true
      case _ => false
    }
}
