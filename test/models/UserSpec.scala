package models

import lib.WithMongoApplication
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

import scala.concurrent.Await

/**
 * Created by trupin on 7/29/14.
 */

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {

  import scala.concurrent.duration._

  "User" should {
    "insert a user" in new WithMongoApplication {
      apply {
        val user = User(email = "toto", username = "toto", password = "toto")

        Await.result[Option[User]](factory.users.create(user).flatMap {
          _ => factory.users.find(user.username, user.password)
        }, Duration(10000, "millis")) must be(Some(user))
      }
    }
  }
}
