package controllers

import jp.t2v.lab.play2.auth.LoginLogout
import lib.oauth.GithubOauth2
import models.{JsonError, JsonFormat, Users}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoPlugin}

import scala.concurrent.Future

/**
 * Created by gaetansenn on 26/07/2014.
 */

object UserFactory {
  lazy val users = Users(ReactiveMongoPlugin.db)
}

class Authentication(users: Users) extends Controller with MongoController with LoginLogout {

  self: AuthConfigImpl =>

  def githubSigning = Action {
    Redirect(GithubOauth2.signIn())
  }

  def success(session_token: String) = Action {
    Ok(Json.obj("status" -> "ok", "session_token" -> session_token))
  }

  def fail(message: String) = Action {
    Ok(Json.obj("status" -> "fail", "message" -> message))
  }

  def githubCallback(code: String) = Action.async { implicit request =>
    GithubOauth2.authenticate(code).flatMap {
      access_token => GithubOauth2.getUser(access_token).map { response =>
        // send a token if the account already exist


        // send a callback request with the form element to register the user
        Redirect(routes.Authentication.success("return_the_token_of_session"))
      }
    }.recover {
      case error: GithubOauth2.UserHandlerError => Redirect(routes.Authentication.fail(error.message))
      case error: GithubOauth2.AccessTokenError => Redirect(routes.Authentication.fail(error.message))
    }
  }

  //  Basic authentication method
  def Authenticate() = Action.async(BodyParsers.parse.json) { implicit request =>

    case class UserLogin(login : String, password: String)

    implicit val UserLoginReads: Reads[UserLogin] = (
      (JsPath \ "login").read[String] and
        (JsPath \ "password").read[String]
      )(UserLogin.apply _)

    val UserLoginResult = request.body.validate[UserLogin]
    UserLoginResult.fold(
      errors => {
        Future.successful(BadRequest(JsonFormat.generateError(JsonError(1234, "", "BadRequest", BAD_REQUEST, "Please refer to the documentation", JsError.toFlatJson(errors).toString(), None))))
      },
      login => {
        users.find(login.login, login.password).flatMap {
          case Some(u) => {
            gotoLoginSucceeded(u._id.toString())
          }
          case _ => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "unknown login or password")))
        }
      }
    )
  }
}

object Authentication extends Authentication(UserFactory.users) with AuthConfigImpl
