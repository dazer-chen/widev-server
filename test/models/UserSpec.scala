package models

import lib.mongo.Mongo
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

/**
  * Created by trupin on 7/29/14.
 */

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification with Mongo with lib.Util {

  sequential

  val users = factory.users

  "User" should {
    "relations" >> {
      users.relations should beEmpty
    }

    ".find" >> {
      val user = users.generate
      result(users.create(user))

      "with username and password" >> {
        result(users.find(user.username, user.password)).get should equalTo(user)
      }
    }

  }
}
