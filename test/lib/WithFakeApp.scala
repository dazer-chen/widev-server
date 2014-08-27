package lib

import play.api.test.{FakeApplication, WithApplication}
import jp.t2v.lab.play2.auth.IdContainer
import jp.t2v.lab.play2.auth.test.Helpers._
import lib.play2auth.AuthConfigMocked
import models.{Permission, User}
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import play.api.test.{FakeApplication, FakeRequest, WithApplication}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 8/15/14.
 */
case class WithFakeApp() extends WithApplication(FakeApplication(withoutPlugins = Seq("play.modules.reactivemongo.ReactiveMongoPlugin"))) {}

trait FakeSession extends Scope with Mockito with AuthConfigMocked {
  def permission: Permission
  val user = User.generate

  val containerMock = mock[IdContainer[Id]]
  containerMock.get(any[String]) returns Some(user._id.stringify)

  trait AuthConfigExtends extends AuthConfigMocked {
    override def resolveUser(id: Id)(implicit concurrentExecutionContext: ExecutionContext): Future[Option[User]] = {
      Future(Some(user))
    }
    override lazy val idContainer: IdContainer[Id] = containerMock
  }

  object config extends AuthConfigMocked

  implicit def fakeRequest = FakeRequest().withLoggedIn(config)(user._id.stringify)
}
