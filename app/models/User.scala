package models

import reactivemongo.bson.{Macros, BSONObjectID}

/**
 * Created by trupin on 7/26/14.
 */
case class User(_id: BSONObjectID = BSONObjectID.generate,
                email: String,
                first_name: String,
                last_name: String,
                name: String,
                password: String,
                github: String)

object User {
  implicit val handler = Macros.handler[User]
}