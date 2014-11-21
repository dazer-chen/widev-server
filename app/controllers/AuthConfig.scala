package controllers

import jp.t2v.lab.play2.auth.{AuthConfig, IdContainer}
import lib.play2auth.MongoIdContainer
import models._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo._
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._

/**
 * Created by gaetansenn on 01/08/2014.
 */

trait AuthConfigImpl extends AuthConfig with Results {

  lazy val factory = {
    import play.api.libs.concurrent.Execution.Implicits._
    Factory(ReactiveMongoPlugin.db)
  }

  /**
   * A type that is used to identify a user.
   * `String`, `Int`, `Long` and so on.
   */
  type Id = String

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
  val sessionTimeoutInSeconds: Int = 3600 * 24 * 10

  /**
   * A function that returns a `User` object from an `Id`.
   * You can alter the procedure to suit your application.
   */
  def resolveUser(id: Id)(implicit context: ExecutionContext): Future[Option[User]] = {
    def db = ReactiveMongoPlugin.db
    Users(db).find(BSONObjectID(id)).map {
      res => res
    }
  }

  /**
   * Where to redirect the user after a successful login.
   */
  def loginSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] = {
    request.cookies.get(cookieName) match {
      case Some(cookie) => Future.successful(Ok(Json.obj("login" -> "success", "token" -> cookie.value)))
      case None => Future.successful(Ok(Json.obj("login" -> "success", "token" -> false)))
    }
  }

  /**
   * Where to redirect the user after logging out
   */
  def logoutSucceeded(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Ok(Json.obj("logout" -> "success")))

  /**
   * If the user is not logged in and tries to access a protected resource then redirect them as follows:
   */
  def authenticationFailed(request: RequestHeader)(implicit ctx: ExecutionContext): Future[Result] =
    Future.successful(Unauthorized(Json.obj("error" -> "resource not allowed")))

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
      case (Administrator, _) => true
      case (p, a) if p.value <= a.value => true
      case _ => false
    }
  }

  override lazy val cookieSecureOption: Boolean = play.api.Play.isProd(play.api.Play.current)

  override lazy val idContainer: IdContainer[Id] = new MongoIdContainer[Id]

}