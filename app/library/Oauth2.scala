package library

import play.api.libs.ws.{WSResponse, WS}

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current


/**
 * Created by gaetansenn on 26/07/2014.
 */
trait Oauth2 {

  case class AccessTokenError(message: String) extends Exception(message)

  val clientId: String
  val clientSecret: String
  val signInUrl: String
  val accessTokenUrl: String
  val redirectUrl: String
  val scope: String
  val responseType: String
  val grantType: String

  def token(resp: WSResponse): String

  def signIn(): String = {
    var url = signInUrl.concat("?client_id=" + clientId)

    if (!redirectUrl.isEmpty) { url = url + "&redirect_uri=" + redirectUrl }
    if (!(responseType.isEmpty)) {url = url + "&response_type=" + responseType }
    if (!scope.isEmpty) {url = url + "&scope=" + scope}

    System.out.println("redirect to %s".format(url))

    return url
  }

  def graphCall(graphUrl: String, accessToken: String): Future[WSResponse] = {
    return WS.url(graphUrl + "?access_token=" + accessToken).get()
  }

  // return a valid access_token
  def authenticate(code: String): Future[String] = {

    var postBody = "client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code

    if (!redirectUrl.isEmpty) {postBody = postBody + "&redirect_uri=" + redirectUrl}
    if (!grantType.isEmpty) {postBody = postBody + "&grant_type=" + grantType}

    for {
    // get access_token from facebook
      accessTokenPromise <- WS.url(accessTokenUrl).withHeaders("Content-Type" -> "application/x-www-form-urlencoded").post(postBody)
      accessToken = token(accessTokenPromise)
    } yield {
      accessToken
    }
  }

}
