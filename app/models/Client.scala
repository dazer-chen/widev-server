package models

import lib.mongo.{SuperCollection, Collection}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by trupin on 7/26/14.
 */
case class Client(
                   _id: BSONObjectID = BSONObjectID.generate,
                   secret: Option[String] = None,
                   redirectUri: Option[String] = None,
                   scope: Option[String] = None,
                   grantTypes: List[GrandType] = List.empty
                   )

object Client {
  implicit val handler = Macros.handler[Client]
}

case class Clients(db: DefaultDB) extends Collection[Client] {
  val collection = db.collection[BSONCollection]("clients")

  def relations: Seq[SuperCollection] = Seq.empty

  def generate: Client = Client(
    secret = Some(BSONObjectID.generate.stringify),
    redirectUri = Some(BSONObjectID.generate.stringify),
    scope = Some(BSONObjectID.generate.stringify),
    grantTypes = List(AuthorizationCodeGrandType, ImplicitGrandType)
  )

  def validate(id: BSONObjectID, secret: String, grandType: GrandType): Future[Boolean] =
    collection.find(BSONDocument("_id" -> id, "secret" -> secret, "grantTypes" -> grandType.value)).one[Client].map {
      case Some(_) => true
      case _ => false
    }

  def find(id: BSONObjectID, secret: String, scope: Option[String]) =
    collection.find(BSONDocument("_id" -> id, "secret" -> secret) ++ (
      if (scope.nonEmpty) BSONDocument("scope" -> scope.get) else BSONDocument()
    )).one[Client]

}
