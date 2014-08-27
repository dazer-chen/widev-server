package lib.play2auth

import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * Created by gaetansenn on 17/08/2014.
 */

class IdContainerMocked[Id: ClassTag] extends IdContainer[Id] {
  def startNewSession(userId: Id, timeoutInSeconds: Int) = ""
  def remove(token: AuthenticityToken) = Unit
  def get(token: AuthenticityToken): Option[Id] = None
  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) = Unit
}

trait AuthConfigMocked extends AuthConfigImpl {
  override lazy val idContainer: IdContainer[Id] = new IdContainerMocked[Id]
  override def resolveUser(id: Id)(implicit ec: ExecutionContext): Future[Option[User]] = Future(None)
}
