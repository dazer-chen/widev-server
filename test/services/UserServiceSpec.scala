package services

import lib.Util
import models.{Users, User}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Future

/**
 * Created by gaetansenn on 17/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class UserServiceSpec extends Specification with Mockito with Util {

  "UserService" should {
    "getUser should return a user with a valid UserID" >> {

      val usersMock = mock[Users]
      val userService = new UserService(usersMock)

      val user = User.generate
      usersMock.find(user._id) returns Future(Some(user))

      result(UserService.find(user._id)) should equalTo(Some(user))

      there was one(usersMock).find(user._id)(concurrentExecutionContext)
    }
  }

}
