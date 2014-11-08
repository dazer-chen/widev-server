package models

import lib.mongo.{Collection, SuperCollection}
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONObjectID, Macros}

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
}

case class Plugins(db: DefaultDB) extends Collection[Plugin] {
  val collection = db.collection[BSONCollection]("plugins")

  def relations: Seq[SuperCollection] = Seq.empty

  override def generate: Plugin = Plugin.generate
}
