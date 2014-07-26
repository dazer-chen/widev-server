package controllers

import library.GithubOauth2
import play.api._
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by gaetansenn on 26/07/2014.
 */
object Authentification extends Controller{

  def githubSigning = Action { Redirect(GithubOauth2.signIn()) }


  def githubCallback(code: String) = Action.async { implicit request =>
    GithubOauth2.authenticate(code).map {
      response => Ok("access_token : %s".format(response))
    }
  }

}
