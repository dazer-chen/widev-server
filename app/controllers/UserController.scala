package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.Authenticated
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import services.UserService

/**
 * Created by gaetansenn on 17/08/2014.
 */
class UserController(userService : UserService) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  def getUser(id: String) = AsyncStack(AuthorityKey -> Authenticated) { request =>
    userService.find(BSONObjectID(id)).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound(s"Couldn't find user for id: $id")
    }
  }
}

object UserController extends UserController(UserService) with AuthConfigImpl
