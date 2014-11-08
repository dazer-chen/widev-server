package models

import lib.mongo.{Collection, SuperCollection}
import lib.util.MD5
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

/**
 * Created by thomastosoni on 8/31/14.
 */

case class BucketFileHeader(
                             path: String,
                             md5: String,
                             createdAt: DateTime = DateTime.now,
                             updatedAt: DateTime = DateTime.now,
                             version: Int = 0
                             )

object BucketFileHeader {
  import lib.util.Implicits._
  implicit val handler = Macros.handler[BucketFileHeader]

  implicit val BucketFileHeaderWrites = new Writes[BucketFileHeader] {
    def writes(model: BucketFileHeader) = Json.obj(
      "path" -> model.path,
      "md5" -> model.md5,
      "createdAt" -> model.createdAt,
      "updatedAt" -> model.updatedAt,
      "version" -> model.version
    )
  }
}

case class Bucket(
											name: String,
											owner: BSONObjectID,
											teams: Set[BSONObjectID] = Set.empty,
                      files: Map[String, BucketFileHeader] = Map.empty,
                      createdAt: DateTime = DateTime.now,
                      updatedAt: DateTime = DateTime.now,
                      version: Int = 0,
	                    _id: BSONObjectID = BSONObjectID.generate
	                  )
{
  def physicalFilePath(filePath: String) = s"${owner.stringify}/${_id.stringify}/${MD5.hex_digest(filePath)}"
}

object Bucket {
  import lib.util.Implicits._

  implicit object BSONBucketFileHeaderMapHandler extends BSONHandler[BSONDocument, Map[String, BucketFileHeader]] {
    override def write(t: Map[String, BucketFileHeader]): BSONDocument = BSONDocument.apply(t.map {
      case (k, fileHeader) => (k, BucketFileHeader.handler.write(fileHeader))
    }.toTraversable)

    override def read(bson: BSONDocument): Map[String, BucketFileHeader] = bson.elements.map {
      case (k, fileHeader: BSONDocument) => (k, BucketFileHeader.handler.read(fileHeader))
    }.toMap
  }

  implicit val handler = Macros.handler[Bucket]

	implicit val BucketWrites = new Writes[Bucket] {
		def writes(model: Bucket) = Json.obj(
			"_id" -> model._id.stringify,
			"name" -> model.name,
			"owner" -> model.owner.stringify,
      "teams" -> model.teams.map(_.stringify),
      "files" -> Json.arr(model.files.map {
        file => Json.toJsFieldJsValueWrapper(file._2)
      }.toSeq:_*),
      "createdAt" -> model.createdAt,
      "updatedAt" -> model.updatedAt,
      "version" -> model.version
		)
	}

	def generate = Bucket(
		name = BSONObjectID.generate.stringify,
		owner = BSONObjectID.generate,
    teams = Set((0 to 10).map(_ => BSONObjectID.generate):_*)
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

  def setFileHeader(id: BSONObjectID, fileHeader: BucketFileHeader) = {
    import lib.util.Implicits.BSONDateTimeHandler

    collection.update(BSONDocument("_id" -> id), BSONDocument(
      "$set" -> BSONDocument(
        s"files.${MD5.hex_digest(fileHeader.path)}" -> fileHeader,
        "updatedAt" -> DateTime.now
      ),
      "$inc" -> BSONDocument(
        "version" -> 1
      )
    ), upsert = true)
  }

  def findByOwner(owner: BSONObjectID) = {
    collection.find(BSONDocument("owner" -> owner)).cursor[Bucket].collect[List]()
  }

  def findFileHeader(id: BSONObjectID, filePath: String): Future[Option[BucketFileHeader]] = {
    val filePathSum = MD5.hex_digest(filePath)

    collection.find(BSONDocument(
      "_id" -> id,
      s"files.$filePathSum" -> BSONDocument(
        "$exists" -> true
      )),
      BSONDocument(s"files.$filePathSum" -> true)
    ).one[BSONDocument].map {
      case Some(doc) =>
        doc.getAs[BSONDocument]("files") match {
          case Some(doc) =>
            doc.getAs[BSONDocument](filePathSum) match {
              case Some(doc) =>
                Some(BucketFileHeader.handler.read(doc))
              case None => None
            }
          case _ => None
        }
      case _ => None
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

  def findBucketInfos(id: BSONObjectID): Future[Option[Bucket]] =
    collection.find(BSONDocument("_id" -> id), BSONDocument("files" -> false)).one[BSONDocument].map {
      case Some(res) =>
        Some(Bucket.handler.read(res ++ ("files" -> BSONDocument())))
      case None => None
    }

  def deleteFileHeader(id: BSONObjectID, filePath: String): Future[Boolean] =
    collection.update(BSONDocument("_id" -> id), BSONDocument(
      "$unset" -> BSONDocument(
        s"files.${MD5.hex_digest(filePath)}" -> "" // WTF were they thinking while designing the mongodb api ?!
      )
    )).map {
      case res if res.updated > 0 => true
      case _ => false
    }

  override def save(model: Bucket): Future[Bucket] =
    super.save(model.copy(updatedAt = DateTime.now(), version = model.version + 1))

  override def update(model: Bucket, upsert: Boolean = true): Future[Boolean] =
    super.update(model.copy(updatedAt = DateTime.now(), version = model.version + 1), upsert)

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
