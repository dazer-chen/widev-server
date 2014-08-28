package controllers

import lib.{FakeSession, Util, WithFakeApp}
import models.{Authenticated, Permission}
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.Helpers._
import services.UserService

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by gaetansenn on 17/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class UserControllerSpec extends Specification with Mockito with Util {

  "UserController" should {
    "getUser should return a json UserModel" >> new WithFakeApp with FakeSession {
      val permission: Permission = Authenticated

      val userServiceMock = mock[UserService]

      userServiceMock.find(user._id)(concurrentExecutionContext) returns Future(Some(user))

      val userController = new UserController() with AuthConfigExtends

      val result = userController.getUser(user._id.stringify)(fakeRequest)

      contentType(result) must equalTo(Some("text/plain"))

      there was one(userServiceMock).find(user._id)(concurrentExecutionContext)
    }
  }
}
