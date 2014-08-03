/**
 * Created by gaetansenn on 02/08/2014.
 */


import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth.test.Helpers._
import lib.WithMongoApplication
import models.User
import org.specs2.mutable.Specification
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Await

class AuthenticationSpec extends Specification {

  object config extends AuthConfigImpl

  implicit val timeout = scala.concurrent.duration.Duration.apply(1000, "milliseconds")

  "Authentication" should {
    "success to connect a user" in new WithMongoApplication {
      apply {
        val user = User(email = "ridertahiti@hotmail.com", username = "ridertahiti", password = "ridertahiti")
        Await.result(factory.users.create(user), timeout) must be(user)

        val res = controllers.Authentication.AuthenticateTest(FakeRequest().withLoggedIn(config)(user._id.stringify))
        contentAsString(res) must contain("ok")
      }
    }
  }

}