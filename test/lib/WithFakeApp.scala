package lib

import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth.test.Helpers._
import jp.t2v.lab.play2.auth.{AuthenticityToken, IdContainer}
import models.{Workspace, Visitor, Permission, User}
import org.specs2.specification.Scope
import play.api.test.{FakeApplication, FakeRequest, WithApplication}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 8/15/14.
 */
class WithFakeApp extends WithApplication(FakeApplication(withoutPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin")))

trait FakeSession extends Scope {
  def permission: Permission
	lazy val currentUser = User.generate.copy(permission = permission)
	lazy val currentWorkspace = Workspace.generate.copy(permission = permission)

  trait AuthConfigMock extends AuthConfigImpl {
    override def resolveUser(id: Id)(implicit concurrentExecutionContext: ExecutionContext): Future[Option[User]] = Future {
      if (permission.value != Visitor.value) Some(currentUser) else None
    }

    override lazy val idContainer: IdContainer[Id] = new IdContainer[Id] {
      override def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = BSONObjectID.generate.stringify

      override def get(token: AuthenticityToken): Option[Id] = Some(currentUser._id.stringify)

      override def remove(token: AuthenticityToken): Unit = Unit

      override def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int): Unit = Unit
    }
  }

  implicit def fakeRequest = FakeRequest().withLoggedIn(new AuthConfigMock {})(currentUser._id.stringify)
}

class WithFakeSessionApp(override val permission: Permission) extends WithFakeApp with FakeSession