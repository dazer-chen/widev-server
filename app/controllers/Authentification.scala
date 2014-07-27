package controllers

import controllers.Application._
import library.GithubOauth2
import play.api._
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection


/**
 * Created by gaetansenn on 26/07/2014.
 */
object Authentification extends Controller with MongoController{

  def collection: BSONCollection = db.collection("users")

  def githubSigning = Action {
    Redirect(GithubOauth2.signIn())
  }

  def githubCallback(code: String) = Action.async { implicit request =>
    GithubOauth2.authenticate(code).flatMap {
      response => GithubOauth2.getUserInformation(response).map { response =>
        // send a token if the account already exist


        // send a callback request with the form element to register the user
//        Ok("test")
        Redirect("http://pipi.lolo?token_session=" + "toto")
      }
    }
  }

}
