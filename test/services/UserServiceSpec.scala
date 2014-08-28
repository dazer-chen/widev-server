package services

import lib.{Util, WithFakeApp}
import models.{User, Users}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by gaetansenn on 17/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class UserServiceSpec extends Specification with Mockito with Util {

  "UserService" should {
    "getUser should return a user with a valid UserID" >> {

      val usersMock = mock[Users]
      val user = User.generate

      usersMock.find(user._id) returns Future(Some(user))

      val userService = new UserService(usersMock)

      result(userService.find(user._id)) should equalTo(Some(user))

      there was one(usersMock).find(user._id)(concurrentExecutionContext)
    }
  }

}
