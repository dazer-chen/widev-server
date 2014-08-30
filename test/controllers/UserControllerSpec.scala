package controllers

import lib.{Util, WithFakeSessionApp}
import models._
import org.specs2._
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.concurrent.Future

/**
 * Created by gaetansenn on 17/08/2014.
 */

class UserControllerSpec extends mutable.Specification with Mockito with Util {

  trait MockFactory extends Scope {
    self: WithFakeSessionApp =>

    val usersMock = mock[Users]
    val userController = new UserController(usersMock) with AuthConfigExtends
  }

  "UserController" should {
    ".getUser" >> {
      "should return a json UserModel" >> new WithFakeSessionApp(Authenticated) with MockFactory {
        usersMock.find(currentUser._id) returns Future(Some(currentUser))

        val result = userController.getUser(currentUser._id.stringify)(fakeRequest)

        contentType(result) must equalTo(Some("application/json"))

        contentAsString(result) must beEqualTo(Json.toJson(currentUser).toString())

        there was one(usersMock).find(currentUser._id)
      }

//      "without a good id should return a bad access" >> new WithFakeSessionApp(Authenticated) with MockFactory {
//        usersMock.find(any[BSONObjectID]) returns Future(None)
//
//        val result = userController.getUser(currentUser._id.stringify)(fakeRequest)
//
//        contentType(result) must equalTo(Some("text/plain"))
//
//        status(result) must equalTo(NOT_FOUND)
//
//        there was one(usersMock).find(currentUser._id)
//      }
    }

//    ".createUser" >> {
//      "should return a json UserModel" >> new WithFakeSessionApp(Visitor) with MockFactory {
//        val fakeUser = User.generate
//
//        usersMock.create(any[User]) returns Future(fakeUser)
//
//        val result = userController
//      }
//    }

  }
}
