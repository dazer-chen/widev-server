package library

import models.User
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import play.core.parsers.FormUrlEncodedParser
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{Future}
import play.api.libs.ws.{DefaultWSResponseHeaders, WSResponse}

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by gaetansenn on 26/07/2014.
 */

trait GitHubAPI {

  val GithubHost = "https://api.github.com/"
  val urlGraph = Map("user" -> "user")

  def getUrl(key: String) = { GithubHost + urlGraph(key) }

  def fetchUser(access_token: String): Future[String]
}


object GithubOauth2 extends Oauth2 with GitHubAPI {

  //  Exception declaration

  case class UserHandlerError(message: String) extends Exception(message)

  val clientId = "c6d7cd1489de805cf8f6"
  val clientSecret = "cd5f677e1cc9e6d97c81f7001dd2f9c48773366f"
  val signInUrl = "https://github.com/login/oauth/authorize"
  val accessTokenUrl = "https://github.com/login/oauth/access_token"
  val redirectUrl = "http://widev-int.herokuapp.com/" + "github/callback"
  val scope = "user"
  val responseType = ""
  val grantType = ""

  //  Oauth2 definitions
  def token(resp: WSResponse): String = {
    println(resp.body)
    FormUrlEncodedParser.parse(resp.body).get("access_token").flatMap(_.headOption) match {
      case Some(test) => test
      case None => throw AccessTokenError(resp.body)
    }
  }

  //  All call from Github return a WSReponse.body

  def fetchUser(access_token: String): Future[String] = {
    GithubOauth2.graphCall(getUrl("user"), access_token).map {
      response => response.body
    }
  }

  //  Convert a github json result to a model
  private case class UserResponse(email: String, name: String, username: String)

  private implicit val userReads: Reads[UserResponse] = (
    (JsPath \ "email").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "login").read[String]
  )(UserResponse.apply _)


  //  Handler functions
  private def UserHandler(response: String): User = {
    println(response)
    Json.parse(response).validate[UserResponse] match {
      case s: JsSuccess[UserResponse] => {
        val response = s.get
        User(lastName = Option(response.name), email = response.email, password = "", username = response.username)
      }
      case e: JsError => throw new UserHandlerError(e.toString)
    }
  }

  def getUser(access_token: String): Future[User] = {
    fetchUser(access_token).map { response =>
      UserHandler(response)
    }
  }

}
