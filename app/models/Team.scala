package models

import controllers.AuthConfigImpl
import lib.mongo.{SuperCollection, Collection}
import lib.util.BearerTokenGenerator
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._


/**
 * Created by gaetansenn on 17/10/14.
 */

case class Team(_id: BSONObjectID = BSONObjectID.generate,
                users: List[BSONObjectID],
                owner: BSONObjectID)

object Team {
  import lib.util.Implicits.BSONDateTimeHandler
  implicit val handler = Macros.handler[Team]

  def generate: Team = Team(
    users = List.fill[BSONObjectID](3)(BSONObjectID.generate),
    owner = BSONObjectID.generate
  )
}

case class Teams(db: DefaultDB) extends Collection[Team] with AuthConfigImpl {

  val collection = db.collection[BSONCollection]("team")

  def relations: Seq[SuperCollection] = Seq.empty

  def generate: Team = Team.generate

}