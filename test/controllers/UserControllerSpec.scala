package controllers

import lib.mongo._
import lib.{Util, WithFakeSessionApp}
import models._
import org.junit.runner.RunWith
import org.specs2._
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.http.HeaderNames
import play.api.libs.iteratee.Input
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Created by gaetansenn on 17/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class UserControllerSpec extends mutable.Specification with Mockito with Util {

  trait MockFactory extends Scope {
    self: WithFakeSessionApp =>

    val usersMock = mock[Users]
    val userController = new UserController(usersMock) with AuthConfigMock
  }

  "UserController" should {
    ".getUser" >> {
      "should return a json UserModel" >> new WithFakeSessionApp(Standard) with MockFactory {
        usersMock.find(any[BSONObjectID]) returns Future(Some(currentUser))

        val result = userController.getUser(currentUser._id.stringify)(fakeRequest)

        contentType(result) must equalTo(Some("application/json"))

        contentAsJson(result) must beEqualTo(Json.toJson(currentUser))

        there was one(usersMock).find(currentUser._id)
      }

      "without a good id should return a bad access" >> new WithFakeSessionApp(Standard) with MockFactory {
        usersMock.find(any[BSONObjectID]) returns Future(None)

        val result = userController.getUser(currentUser._id.stringify)(fakeRequest)

        status(result) must equalTo(NOT_FOUND)

        there was one(usersMock).find(currentUser._id)
      }

      "without an authenticated user should return an unauthorized error" >> new WithFakeSessionApp(Visitor) with MockFactory {
        val result = userController.getUser(currentUser._id.stringify)(fakeRequest)

        status(result) must equalTo(UNAUTHORIZED)
      }
    }

    ".createUser" >> {
      "should return a json UserModel" >> new WithFakeSessionApp(Visitor) with MockFactory {
        usersMock.create(any[User]) returns Future(currentUser)

        val json = Json.obj(
          "email" -> JsString(currentUser.email),
          "password" -> JsString(currentUser.password)
        )

        val body    = "{\"email\": \"" + currentUser.email + "\", \"password\": \"" + currentUser.password + "\" }"
        val request = FakeRequest().withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = userController.createUser().apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        contentType(result) must equalTo(Some("application/json"))

        there was one(usersMock).create(any[User])
      }


      "with a duplicate user, should return an error" >> new WithFakeSessionApp(Visitor) with MockFactory {
        usersMock.create(any[User]) returns Future.failed(new DuplicateModel("duplicate user"))

        val body    = "{\"email\": \"" + currentUser.email + "\", \"password\": \"" + currentUser.password + "\" }"
        val request = FakeRequest().withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = userController.createUser().apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(NOT_ACCEPTABLE)

        there was one(usersMock).create(any[User])
      }
    }
  }
}
