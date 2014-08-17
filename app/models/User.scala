package models

import lib.mongo.{Collection, SuperCollection}
import play.api.libs.json.{Json, Writes}
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

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
                 permission: Permission = Visitor
                 )

object User {
  implicit val handler = Macros.handler[User]

  //Json write Handler
  implicit val UserWrites = new Writes[User] {
    def writes(model: User) = Json.obj(
      "email" -> model.email,
      "username" -> model.username,
      "password" -> model.password,
      "firstName" -> model.firstName,
      "lastName" -> model.lastName
    )
  }

  def generate = User(
    email = BSONObjectID.generate.stringify,
    username = BSONObjectID.generate.stringify,
    password = BSONObjectID.generate.stringify
  )

}

case class Users(db: DefaultDB) extends Collection[User] {
  val collection = db.collection[BSONCollection]("users")

  def relations: Seq[SuperCollection] = Seq.empty

  def generate: User = User.generate

  def find(username: String, password: String)(implicit ec: ExecutionContext): Future[Option[User]] =
    collection.find(BSONDocument("username" -> username, "password" -> password)).one[User]
}