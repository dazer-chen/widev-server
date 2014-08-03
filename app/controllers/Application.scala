package controllers

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import models._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc._
import play.modules.reactivemongo._

import scala.concurrent.Future

object Application extends Controller with LoginLogout with AuthConfigImpl {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def loginSuccess = Action {
    Ok(Json.obj("login" -> "success"))
  }

  def loginFail = Action {
    Ok(Json.obj("login" -> "fail"))
  }

}