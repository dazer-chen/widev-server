package controllers

/**
 * Created by gaetansenn on 02/08/2014.
 */

import jp.t2v.lab.play2.auth._
import lib.{Util, WithFakeApp}
import models.{User, Users}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.http.HeaderNames
import play.api.libs.iteratee.Input
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class IdContainerMocked[Id: ClassTag] extends IdContainer[Id] {
  def startNewSession(userId: Id, timeoutInSeconds: Int) = ""
  def remove(token: AuthenticityToken) = Unit
  def get(token: AuthenticityToken) = None
  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int) = Unit

}

trait authMock extends AuthConfigImpl {
  override lazy val idContainer: IdContainer[Id] = new IdContainerMocked[Id]
}

@RunWith(classOf[JUnitRunner])
class AuthenticationSpec extends Specification with Mockito with Util {

  object config extends AuthConfigImpl

  implicit val timeout = scala.concurrent.duration.Duration.apply(1000, "milliseconds")

  "AuthenticationSpec" should {

    "Should redirect to success when authentication success" >> new WithFakeApp {
      val userMock = mock[Users]
      val user = User.generate

      when(userMock.find(any[String], any[String])(any[ExecutionContext])).thenReturn(Future(Some(user)))
      class MockedAuthentication extends Authentication(userMock) with authMock
      val controller = spy(new MockedAuthentication)
      val body    = "{\"login\": \"" + user.username + "\", \"password\": \"" + user.password + "\" }"
      val request = FakeRequest().withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
      val res: Future[Result] = controller.Authenticate.apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)
      status(res) must equalTo(OK)

    }

    "Should return a 400 when authentication fail" >> new WithFakeApp {
      val userMock = mock[Users]
      val user = User.generate

      when(userMock.find(any[String], any[String])(any[ExecutionContext])).thenReturn(Future(None))
      class MockedAuthentication extends Authentication(userMock) with authMock
      val controller = spy(new MockedAuthentication)
      val body    = "{\"login\": \"" + user.username + "\", \"password\": \"" + user.password + "\" }"
      val request = FakeRequest().withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
      val res: Future[Result] = controller.Authenticate.apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)
      status(res) must equalTo(BAD_REQUEST)
    }
  }
}