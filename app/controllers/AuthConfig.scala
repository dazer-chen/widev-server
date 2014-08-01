package controllers

import jp.t2v.lab.play2.auth.{CookieIdContainer, IdContainer, AuthConfig}
import play.api.libs.json.Json
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._
import play.api.mvc.{Result, SimpleResult, RequestHeader}
import jp.t2v.lab.play2.auth.AuthConfig
import play.modules.reactivemongo._
import models.{NormalUser, Administrator, Permission, Users}
import play.api.mvc._


import play.api.Play.current

/**
 * Created by gaetansenn on 01/08/2014.
 */

trait AuthConfigImpl extends AuthConfig with Results {

  /**
   * A type that is used to identify a user.
   * `String`, `Int`, `Long` and so on.
   */
  type Id = BSONObjectID

  /**
   * A type that represents a user in your application.
   * `User`, `Account` and so on.
   */
  type User = models.User

  /**
   * A type that is defined by every action for authorization.
   * This sample uses the following trait:
   *
   * sealed trait Permission
   * case object Administrator extends Permission
   * case object NormalUser extends Permission
   */
  type Authority = Permission

  /**
   * A `ClassTag` is used to retrieve an id from the Cache API.
   * Use something like this:
   */
  val idTag: ClassTag[Id] = classTag[Id]

  /**
   * The session timeout in seconds
   */
  val sessionTimeoutInSeconds: Int = 3600

  /**
   * A function that returns a `User` object from an `Id`.
   * You can alter the procedure to suit your application.
   */
  def resolveUser(id: Id)(implicit context: ExecutionContext): Future[Option[User]] = {
    def db = ReactiveMongoPlugin.db
    Users(db).findById(id)
  }

  /**
   * Where to redirect the user after a successful login.
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Ok(Json.obj("login" -> "success")))

  /**
   * Where to redirect the user after logging out
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Ok(Json.obj("logout" -> "success")))

  /**
   * If the user is not logged in and tries to access a protected resource then redirect them as follows:
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Ok(Json.obj("error" -> "resource not allowed")))

  /**
   * If authorization failed (usually incorrect password) redirect the user as follows:
   */
  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Forbidden("no permission"))

  /**
   * A function that determines what `Authority` a user has.
   * You should alter this procedure to suit your application.
   */
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext): Future[Boolean] = Future.successful {
    (user.permission, authority) match {
      case (Administrator, _)       => true
      case (NormalUser, NormalUser) => true
      case _                        => false
    }
  }

  /**
   * Whether use the secure option or not use it in the cookie.
   * However default is false, I strongly recommend using true in a production.
   */
  override lazy val cookieSecureOption: Boolean = play.api.Play.isProd(play.api.Play.current)

}