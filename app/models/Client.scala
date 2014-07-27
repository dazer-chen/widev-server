package models

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

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

  def validate(id: BSONObjectID, secret: String, grandType: GrandType)(implicit collection: BSONCollection): Future[Boolean] =
    collection.find(BSONDocument("_id" -> id, "secret" -> secret, "grandType" -> grandType.value)).one[Client].map {
      case Some(_) => true
      case _ => false
    }


}
