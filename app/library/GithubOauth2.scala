package library

import models.User
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import play.core.parsers.FormUrlEncodedParser
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{Future}
import play.api.libs.ws.{WSResponse}

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by gaetansenn on 26/07/2014.
 */


object GithubOauth2 extends Oauth2 {

  val clientId = "c6d7cd1489de805cf8f6"
  val clientSecret = "cd5f677e1cc9e6d97c81f7001dd2f9c48773366f"
  val signInUrl = "https://github.com/login/oauth/authorize"
  val accessTokenUrl = "https://github.com/login/oauth/access_token"
  val redirectUrl = "http://widev-int.herokuapp.com/" + "github/callback"
  val scope = "user"
  val responseType = ""
  val grantType = ""

  case class Response (email: String, name: String, username: String)

  implicit val userReads: Reads[Response] = (
    (JsPath \ "email").read[String] and
    (JsPath \ "name").read[String] and
    (JsPath \ "username").read[String]
  )(Response.apply _)

  def token(resp: WSResponse): String = {
    println(resp.body)
    FormUrlEncodedParser.parse(resp.body).get("access_token").flatMap(_.headOption) match {
      case Some(test) => test
      case None => throw library.AccessTokenError
    }
  }

  def getUserInformation(access_token: String): Future[User] = {
    GithubOauth2.graphCall("https://api.github.com/user", access_token).map { response =>
      Json.parse(response.body).validate[Response] match {
        case s: JsSuccess[Response] => {
          val response = s.get
          User(lastName = Option(response.name), email = response.email, password = "", username = response.username)
        }
        case e: JsError => throw new RuntimeException(e.toString)
      }

    }
  }
}
