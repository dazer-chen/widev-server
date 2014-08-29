package controllers

import lib.{FakeSession, Util, WithFakeApp}
import models.{Authenticated, Permission}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import services.UserService

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by gaetansenn on 17/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class UserControllerSpec extends Specification with Mockito with Util {

  "UserController" should {
    "getUser should return a json UserModel" >> new WithFakeApp with FakeSession {
      def permission: Permission = Authenticated

      val userServiceMock = mock[UserService]

      userServiceMock.find(any[BSONObjectID])(any[ExecutionContext]) returns Future(Some(user))

      val userController = new UserController(userServiceMock) with AuthConfigExtends

      val result = userController.getUser(user._id.stringify)(fakeRequest)

      contentType(result) must equalTo(Some("application/json"))

      contentAsString(result) must beEqualTo(Json.toJson(user).toString())

      //Not working why i don't really know
      there was one(userServiceMock).find(BSONObjectID.generate)(concurrentExecutionContext)
    }

    "getUser without a good id should return a bad access" >> new WithFakeApp with FakeSession {
      def permission: Permission = Authenticated

      val userServiceMock = mock[UserService]

      userServiceMock.find(any[BSONObjectID])(any[ExecutionContext]) returns Future(None)


      val userController = new UserController(userServiceMock) with AuthConfigExtends

      val result = userController.getUser(user._id.stringify)(fakeRequest)

      contentType(result) must equalTo(Some("text/plain"))

      status(result) must equalTo(NOT_FOUND)

    }
  }
}
