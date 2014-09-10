package lib.play2auth

import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth.AuthConfig
import play.api.libs.Crypto
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Controller, Cookie, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by gaetansenn on 10/09/14.
 */

trait LoginSuccess {
  self: Controller with AuthConfig =>

  def gotoLoginSucceeded(userId: Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    val token = idContainer.startNewSession(userId, sessionTimeoutInSeconds)
    val value = Crypto.sign(token) + token
    Future(Ok(Json.obj("login" -> "success", "token" -> value)))
  }

}