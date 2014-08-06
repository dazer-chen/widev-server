package models

import lib.mongo.{SuperCollection, Collection}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.{ExecutionContext, Future}

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

  def generate: Client = Client()

  def validate(id: BSONObjectID, secret: String, grandType: GrandType)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.find(BSONDocument("_id" -> id, "secret" -> secret, "grandType" -> grandType.value)).one[Client].map {
      case Some(_) => true
      case _ => false
    }

  def find(id: BSONObjectID, secret: String, scope: Option[String])(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("_id" -> id, "secret" -> secret) ++ (
      if (scope.nonEmpty) BSONDocument("scope" -> scope.get) else BSONDocument()
    )).one[Client]

}
