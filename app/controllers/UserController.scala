package controllers

import jp.t2v.lab.play2.auth.{LoginLogout, AuthElement}
import lib.mongo.DuplicateModel
import models.{Standard, User, Users}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsError, Reads, JsPath, Json}
import play.api.mvc._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID
import play.api.libs.functional.syntax._

import scala.concurrent.Future


/**
 * Created by gaetansenn on 17/08/2014.
 */
class UserController(users: Users) extends Controller with AuthElement with LoginLogout {
  self: AuthConfigImpl =>

  def getUser(id: String) = AsyncStack(AuthorityKey -> Standard) {
    request =>
      users.find(BSONObjectID(id)).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound(s"Couldn't find user for id: $id")
      }
  }

  /* parameters email, password, username */
  def createUser = Action.async(BodyParsers.parse.json) { implicit request =>

    case class createUser(email: String, password: String, username : String)

    implicit val createUserReads: Reads[createUser] = (
      (JsPath \ "email").read[String] and
        (JsPath \ "password").read[String] and
        (JsPath \ "username").read[String]
      )(createUser.apply _)

    val user = request.body.validate[createUser]

    user.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      user => {
        users.create(User(
          email = user.email,
          password = user.password,
          username = user.username
        )).flatMap {
          user => {
	          println("user id is : " + user._id.stringify)
	          gotoLoginSucceeded(user._id.stringify)
          }
        } recover {
          case err: DuplicateModel => BadRequest(Json.obj("status" -> "KO", "message" -> "user already exists"))
        }
      }
    )
  }
}

object UserController extends UserController(Users(ReactiveMongoPlugin.db)) with AuthConfigImpl
