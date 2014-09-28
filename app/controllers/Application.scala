package controllers

import java.nio.ByteBuffer

import jp.t2v.lab.play2.auth.AuthElement
import models._
import play.api.libs.json.Json
import play.api.mvc._
import utils.RequestHandler

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

  def AuthenticateTest = StackAction(AuthorityKey -> Visitor) { response =>
    Ok("ok")
  }

  def test = Action {
    val json = "{\"filename\":\"toto\",\"size\":2,\"offset\":2,\"mode\": \"Read\"}"
    val jsonsize = ByteBuffer.allocate(4).putInt(json.length).array()

    RequestHandler.read(Array[Byte]())
    Ok("ok")
  }

}