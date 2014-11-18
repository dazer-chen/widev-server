package models

import lib.mongo.{Collection, SuperCollection}
import org.joda.time.DateTime
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 11/8/14.
 */
case class Plugin(
                   name: String,
                   endPoint: String,
                   _id: BSONObjectID = BSONObjectID.generate
                   )

object Plugin {
  implicit val handler = Macros.handler[Plugin]

  def generate: Plugin = Plugin(
    name = BSONObjectID.generate.stringify,
    endPoint = BSONObjectID.generate.stringify
  )

  implicit val writes = new Writes[Plugin] {
    def writes(model: Plugin) = Json.obj(
      "name" -> model.name,
      "endPoint" -> model.endPoint,
      "_id" -> model._id.stringify
    )
  }
}

case class Plugins(db: DefaultDB) extends Collection[Plugin] {

  val collection = db.collection[BSONCollection]("plugins")

  def relations: Seq[SuperCollection] = Seq.empty

  override def generate: Plugin = Plugin.generate

  def list = collection.find(BSONDocument()).cursor[Plugin].collect[List]()
}
