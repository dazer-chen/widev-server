package controllers

import jp.t2v.lab.play2.auth.{AuthElement, LoginLogout}
import models._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc._
import play.modules.reactivemongo._

import scala.concurrent.Future

object Application extends Controller with MongoController with LoginLogout with AuthElement with AuthConfigImpl {

  val users = Users(db)

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def loginSuccess = Action {
    Ok(Json.obj("login" -> "success"))
  }

  def loginFail = Action {
    Ok(Json.obj("login" -> "fail"))
  }

  def test = StackAction(AuthorityKey -> Authenticated) { implicit request =>
    Ok("ok")
  }

  def authenticate = Action.async(BodyParsers.parse.json) { implicit request =>

    case class UserLogin(login : String, password: String)

    implicit val UserLoginReads: Reads[UserLogin] = (
      (JsPath \ "login").read[String] and
        (JsPath \ "password").read[String]
      )(UserLogin.apply _)

    val UserLoginResult = request.body.validate[UserLogin]
    UserLoginResult.fold(
      errors => {
        Future.successful(BadRequest(JsonFormat.generateError(JsonError(1234, "", "BadRequest", BAD_REQUEST, "Please refer to the documentation", JsError.toFlatJson(errors).toString(), None))))
      },
      login => {
        users.find(login.login, login.password).flatMap {
          case Some(u) => gotoLoginSucceeded(u._id.toString())
          case _ => Future.successful(BadRequest(Json.obj("status" -> "KO", "message" -> "login or passoword incorect")))
        }
      }
    )
  }

}