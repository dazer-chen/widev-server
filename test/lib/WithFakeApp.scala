package lib

import jp.t2v.lab.play2.auth.test.Helpers._
import jp.t2v.lab.play2.auth.{AuthenticityToken, IdContainer}
import lib.play2auth.AuthConfigMocked
import models.{Permission, User}
import org.specs2.specification.Scope
import play.api.test.{FakeApplication, FakeRequest, WithApplication}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 8/15/14.
 */
class WithFakeApp extends WithApplication(FakeApplication(withoutPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin")))

trait FakeSession extends Scope with AuthConfigMocked {
  def permission: Permission
  lazy val currentUser = User.generate.copy(permission = permission)

  val fakeContainerId = new IdContainer[Id] {
    override def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = ???

    override def get(token: AuthenticityToken): Option[Id] = Some(currentUser._id.stringify)

    override def remove(token: AuthenticityToken): Unit = ???

    override def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int): Unit = ???
  }

  trait AuthConfigExtends extends AuthConfigMocked {
    override def resolveUser(id: Id)(implicit concurrentExecutionContext: ExecutionContext): Future[Option[User]] = {
      Future(Some(currentUser))
    }
    override lazy val idContainer: IdContainer[Id] = fakeContainerId
  }

  object config extends AuthConfigMocked

  implicit def fakeRequest = FakeRequest().withLoggedIn(config)(currentUser._id.stringify)
}

class WithFakeSessionApp(override val permission: Permission) extends WithFakeApp with FakeSession