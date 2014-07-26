package models

import reactivemongo.bson.{BSONObjectID, Macros}

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
