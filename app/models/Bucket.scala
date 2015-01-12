package models

import lib.mongo.Collection
import org.joda.time.{DateTimeZone, DateTime}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.LastError
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import lib.util.Implicits._

case class File(
                 path: String,
                 encoding: String,
                 id: Int
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

  implicit val fileReads = (
    (JsPath \ "path").read[String] and
      (JsPath \ "encoding").read[String] and
      (JsPath \ "id").read[Int]
    )(File.apply _)
}

case class Informations(
                         name: String,
                         version: Int
                         )

object Informations {
  implicit val handler = Macros.handler[File]

  implicit val informationsReads = (
    (JsPath \ "name").read[String] and
      (JsPath \ "version").read[Int]
    )(Informations.apply _)

  implicit val informationsWrites = new Writes[Informations] {
    override def writes(o: Informations): JsValue = Json.obj(
      "name" -> o.name,
      "version" -> o.version
    )
  }
}

case class NavigatorElement(
                             children: Array[Int] = Array.empty,
                             id: Int,
                             kind: Int,
                             name: String,
                             path: String
                             )

object NavigatorElement {
  implicit val handler = Macros.handler[NavigatorElement]

  implicit val navigatorElementReads = (
    (JsPath \ "children").read[Array[Int]] and
      (JsPath \ "id").read[Int] and
      (JsPath \ "kind").read[Int] and
      (JsPath \ "name").read[String] and
      (JsPath \ "path").read[String]
    )(NavigatorElement.apply _)

  implicit val navigatorElementWrites = new Writes[NavigatorElement] {
    override def writes(o: NavigatorElement): JsValue = Json.obj(
      "children" -> o.children,
      "id" -> o.id,
      "kind" -> o.kind,
      "name" -> o.name,
      "path" -> o.path
    )
  }
}

case class Project(
                    cmakelistsCache: String,
                    cmakelistsPath: String,
                    makefilePath: String
                    )

object Project {
  implicit val handler = Macros.handler[Project]

  implicit val projectReads = (
    (JsPath \ "cmakelists_cache").read[String] and
      (JsPath \ "cmakelists_path").read[String] and
      (JsPath \ "makefile_path").read[String]
    )(Project.apply _)

  implicit val projectWrites = new Writes[Project] {
    override def writes(o: Project): JsValue = Json.obj(
      "cmakelists_cache" -> o.cmakelistsCache,
      "cmakelists_path" -> o.cmakelistsPath,
      "makefile_path" -> o.makefilePath
    )
  }
}

case class Target(
                   arguments: Array[String],
                   dependencies: Array[String],
                   environment: String,
                   execPath: String,
                   isHidden: Boolean,
                   isProduct: Boolean,
                   name: String,
                   workingDirectory: String
                   )

object Target {
  implicit val handler = Macros.handler[Target]

  implicit val targetReads = (
    (JsPath \ "arguments").read[Array[String]] and
      (JsPath \ "dependencies").read[Array[String]] and
      (JsPath \ "environment").read[String] and
      (JsPath \ "execPath").read[String] and
      (JsPath \ "isHidden").read[Boolean] and
      (JsPath \ "isProduct").read[Boolean] and
      (JsPath \ "name").read[String] and
      (JsPath \ "workingDirectory").read[String]
    )(Target.apply _)

  implicit val targetsWrites = new Writes[Target] {
    override def writes(o: Target): JsValue = Json.obj(
      "arguments" -> o.arguments,
      "dependencies" -> o.dependencies,
      "environment" -> o.environment,
      "execPath" -> o.execPath,
      "isHidden" -> o.isHidden,
      "isProduct" -> o.isProduct,
      "name" -> o.name,
      "workingDirectory" -> o.workingDirectory
    )
  }
}

case class Bucket(
                   name: String,
                   owner: BSONObjectID,
                   version: Int = 0,
                   teams: Set[BSONObjectID] = Set.empty,
                   creation: DateTime = DateTime.now(DateTimeZone.UTC),
                   update: DateTime = DateTime.now(DateTimeZone.UTC),
                   files: Array[File] = Array.empty,
                   _id: BSONObjectID = BSONObjectID.generate,
                   navigator: Array[NavigatorElement],
                   project: Project,
                   targets: Array[Target]
                   )

object Bucket {
  implicit val handler = Macros.handler[Bucket]

  implicit val BucketWrites = new Writes[Bucket] {
    override def writes(o: Bucket) = Json.obj(
      "id" -> o._id.stringify,
      "files" -> o.files,
      "informations" -> Json.obj(
        "creation" -> o.creation,
        "update" -> o.update,
        "name" -> o.name,
        "version" -> o.version
      ),
      "navigator" -> o.navigator,
      "project" -> o.project,
      "targets" -> o.targets
    )
  }

  case class CreateBucket(
                           informations: Informations,
                           files: Array[File],
                           navigator: Array[NavigatorElement],
                           project: Project,
                           targets: Array[Target]
                           )

  implicit val createBucketReads = (
    (JsPath \ "informations").read[Informations] and
      (JsPath \ "files").read[Array[File]] and
      (JsPath \ "navigator").read[Array[NavigatorElement]] and
      (JsPath \ "project").read[Project] and
      (JsPath \ "targets").read[Array[Target]]
    )(CreateBucket.apply _)

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

  def find(name: String): Future[Option[Bucket]] =
    collection.find(
      BSONDocument("name" -> name)
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
            collection.find(BSONDocument(
              "teams" -> BSONDocument("$in" -> otherTeams.map(_._id)))
            ).cursor[Bucket].collect[Set]().map(_ ++ ownedBuckets)
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
    collection.find(BSONDocument(
      "_id" -> bucketId, "files.path" -> path
    ), BSONDocument(
      "files.$" -> 1
    )).one[BSONDocument].map {
      case Some(document) =>
        document.getAs[Array[File]]("files").map {
          case files if files.size > 0 =>
            files(0)
        }
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

  def addFileToBucket(bucketId: BSONObjectID, filePath: String, encoding: String): Future[Boolean] = {
    def update(id: Int) = collection.update(BSONDocument("_id" -> bucketId), BSONDocument(
      "$push" -> BSONDocument(
        "files" -> File(filePath, encoding, id)
      )
    )).map(_ => true)

    findFileInBucket(bucketId, filePath).flatMap {
      case Some(file) =>
        removeFileFromBucket(bucketId, filePath).flatMap(_ => update(file.id))
      case None => update(0)
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

  def markAsUpdated(id: BSONObjectID) = collection.uncheckedUpdate(
    BSONDocument("_id" -> id), BSONDocument(
      "$set" -> BSONDocument(
        "update" -> DateTime.now(DateTimeZone.UTC)
      )
    )
  )
}
