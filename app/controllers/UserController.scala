package controllers

import jp.t2v.lab.play2.auth.{AsyncAuth, LoginLogout, AuthElement}
import lib.mongo.DuplicateModel
import lib.play2auth.LoginSuccess
import managers.PluginManager
import models.{Buckets, Standard, User, Users}
import org.mindrot.jbcrypt.BCrypt
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

	def getUsers = Action.async { request =>
		val q = request.getQueryString("q")

		if (q.isEmpty)
			Future(BadRequest(s"'q' parameter required."))
		else
			users.findByQ(q.get).map(users => Ok(Json.toJson(users)))
	}

  def getCurrentUser = optionalUserAction.async { user => implicit request =>
    user match {
      case Some(account) => Future(Ok(Json.toJson(user)))
      case None => Future(NotFound(s"Couldn't find current user, please check if your session is active"))
    }
  }

  /* parameters email, password, username */
  def createUser = Action.async(BodyParsers.parse.json) { request =>

    case class createUser(email: String, password: String, firstName: Option[String], lastName: Option[String])

    implicit val createUserReads: Reads[createUser] = (
      (JsPath \ "email").read[String] and
        (JsPath \ "password").read[String] and
        (JsPath \ "firstName").read[Option[String]] and
        (JsPath \ "lastName").read[Option[String]]
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
          firstName = user.firstName,
          lastName = user.lastName)
        ).flatMap {
          user => {
            PluginManager.createUser(user).flatMap {
              _ => gotoLoginSucceeded(user._id.stringify)(request, defaultContext)
            }
          }
        } recover {
          case err: DuplicateModel =>
            NotAcceptable(s"User already exists.")
        }
      }
    )
  }

}

object UserController extends UserController(Users(ReactiveMongoPlugin.db)) with AuthConfigImpl
