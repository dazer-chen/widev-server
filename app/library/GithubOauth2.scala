package library

import play.api.libs.ws.WSResponse
import play.core.parsers.FormUrlEncodedParser

/**
 * Created by gaetansenn on 26/07/2014.
 */


object GithubOauth2 extends Oauth2 {

  val clientId = "c6d7cd1489de805cf8f6"
  val clientSecret = "cd5f677e1cc9e6d97c81f7001dd2f9c48773366f"
  val signInUrl = "https://github.com/login/oauth/authorize"
  val accessTokenUrl = "https://github.com/login/oauth/access_token"
  val redirectUrl = play.Play.application().configuration().getString("application.url") + "/github/callback"
  val scope = "user"
  val responseType = ""
  val grantType = ""

  def token(resp: WSResponse): String = {
    FormUrlEncodedParser.parse(resp.body).get("access_token").flatMap(_.headOption) match {
      case Some(test) => test
      case None => throw library.AccessTokenError
    }
  }
}
