package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.{Authenticated, User, Users, Visitor}
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

/**
 * Created by gaetansenn on 17/08/2014.
 */
class UserController(users: Users) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  def getUser(id: String) = AsyncStack(AuthorityKey -> Authenticated) {
    request =>
      users.find(BSONObjectID(id)).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound(s"Couldn't find user for id: $id")
      }
  }

  def create(email: String, password: String, username: String) = AsyncStack(AuthorityKey -> Visitor) {
    request =>
      users.create(User(email, password, username)).map {
        user => Ok(Json.toJson(user))
      }
  }
}

object UserController extends UserController(Users(ReactiveMongoPlugin.db)) with AuthConfigImpl
