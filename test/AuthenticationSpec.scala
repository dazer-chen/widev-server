/**
 * Created by gaetansenn on 02/08/2014.
 */


import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth.test.Helpers._
import lib.MongoSpec
import models.{User, Users}
import play.api.test.Helpers._
import play.api.test._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await

class AuthenticationSpec extends MongoSpec {

  object config extends AuthConfigImpl

  implicit val timeout = scala.concurrent.duration.Duration.apply(1000, "milliseconds")

  "Authentication" should {
    isolated
    "context" >> {
      val users = Users(ReactiveMongoPlugin.db)

      val user = User(email = "ridertahiti@hotmail.com", username = "ridertahiti", password = "ridertahiti")
      Await.result(users.create(user), timeout) must be(user)

//      "success to connect a user" >> {
//        val res = controllers.Application.test(FakeRequest().withLoggedIn(config)(user._id.stringify))
//        contentAsString(res) must contain("ok")
//      }
//
//      "fail to connect a user" >> {
//        val res = controllers.Application.test(FakeRequest().withLoggedIn(config)(BSONObjectID.generate.stringify))
//        status(res) must equalTo(UNAUTHORIZED)
//      }
    }
  }
}