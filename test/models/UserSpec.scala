package models

import lib.mongo.Mongo
import org.specs2.mutable._

/**
  * Created by trupin on 7/29/14.
 */

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
        result(users.find(user.email, user.password)).get should equalTo(user)
      }
    }

  }
}
