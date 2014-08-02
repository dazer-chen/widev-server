package models

import lib.mongo.Collection
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

import scala.concurrent.{Future, ExecutionContext}

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
                 gitHub: Option[String] = None,
                 permission: Permission = Authenticated
                 )

object User {
  import Permission._
  implicit val handler = Macros.handler[User]
}

case class Users(db: DefaultDB) extends Collection {
  val collection = db.collection[BSONCollection]("users")

  def create(user: User)(implicit ec: ExecutionContext): Future[User] =
    collection.insert[User](user).map { _ => user }

  def findById(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[User]] =
    collection.find(BSONDocument("_id" -> id)).one[User]

  def find(username: String, password: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    collection.find(BSONDocument("name" -> username, "password" -> password)).one[User]
}