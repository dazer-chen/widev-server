package models

import lib.Collection
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.ExecutionContext

/**
 * Created by trupin on 7/26/14.
 */

case class User(
                 _id: BSONObjectID = BSONObjectID.generate,
                 email: String,
                 username: String,
                 password: String,
                 firstName: Option[String] = None,
                 lastName: Option[String] = None,
                 gitHub: Option[String] = None
                 )

object User {
  implicit val handler = Macros.handler[User]
}

case class Users(db: DefaultDB) extends Collection(db) {
  val collectionName = "users"

  def findUser(name: String, password: String)(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("name" -> name, "password" -> password)).one[User]
}