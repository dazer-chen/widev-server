package controllers

import lib.oauth.GithubOauth2
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection

import scala.concurrent.Future

/**
 * Created by gaetansenn on 26/07/2014.
 */
object Authentication extends Controller with MongoController {
  def collection: BSONCollection = db.collection("users")

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

}
