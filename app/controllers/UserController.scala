package controllers

import jp.t2v.lab.play2.auth.{AsyncAuth, LoginLogout, AuthElement}
import lib.mongo.DuplicateModel
import lib.play2auth.LoginSuccess
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
class UserController(users: Users) extends Controller with AuthElement with LoginSuccess with AsyncAuth {
  self: AuthConfigImpl =>

  def getUser(id: String) = AsyncStack(AuthorityKey -> Standard) { request =>
      users.find(BSONObjectID(id)).map {
        case Some(user) => Ok(Json.toJson(user))
        case None => NotFound(s"Couldn't find user for id: $id")
      }
  }

  def getCurrentUser = optionalUserAction.async { user => implicit request =>
    user match {
      case Some(account) => Future(Ok(Json.toJson(user)))
      case None => Future(NotFound(s"Couldn't find current user, please check if your session is active"))
    }
  }

  /* parameters email, password, username */
  def createUser = Action.async(BodyParsers.parse.json) { request =>

    case class createUser(email: String, password: String)

    implicit val createUserReads: Reads[createUser] = (
      (JsPath \ "email").read[String] and
        (JsPath \ "password").read[String]
      )(createUser.apply _)

    val user = request.body.validate[createUser]

    user.fold(
      errors => {
        Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
      },
      user => {

        users.create(User(user.email, user.password)).flatMap {
          user => gotoLoginSucceeded(user._id.stringify)(request, defaultContext)
        } recover {
          case err: DuplicateModel =>
            NotAcceptable(s"User already exists.")
        }
      }
    )
  }
}

object UserController extends UserController(Users(ReactiveMongoPlugin.db)) with AuthConfigImpl
