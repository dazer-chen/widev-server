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

//  "User" should {
//    ".create" >> {
//      val user = factory.users.user
//      result(factory.users.create(user)) must be(user)
//    }
//
//  }
}
