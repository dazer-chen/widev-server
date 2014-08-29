package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.Authenticated
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID
import services.UserService
import play.api.Play.current

import scala.concurrent.Future

/**
 * Created by gaetansenn on 17/08/2014.
 */
class UserController(userService : UserService) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  def getUser(id: String) = AsyncStack(AuthorityKey -> Authenticated) { request =>
    userService.find(BSONObjectID(id)).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound("")
    }
  }
}

object UserController extends UserController(UserService) with AuthConfigImpl
