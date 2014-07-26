package models

import reactivemongo.bson.{Macros, BSONObjectID}

/**
 * Created by trupin on 7/26/14.
 */
case class User(_id: BSONObjectID = BSONObjectID.generate, name: String, hashedPassword: String)

object User {
  implicit val handler = Macros.handler[User]
}