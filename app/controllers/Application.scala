package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models._
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller with AuthElement with AuthConfigImpl {

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