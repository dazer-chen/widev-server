package models

import reactivemongo.bson.{Macros, BSONObjectID}

/**
 * Created by trupin on 7/26/14.
 */
case class User(
                 _id: BSONObjectID = BSONObjectID.generate,
                 email: String,
                 name: String,
                 password: String,
                 firstName: Option[String] = None,
                 lastName: Option[String] = None,
                 gitHub: Option[String] = None
                 )

object User {
  implicit val handler = Macros.handler[User]
}