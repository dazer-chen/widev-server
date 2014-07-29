package lib.oauth

import lib.oauth.GithubOauth2.UserHandlerError
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication

/**
 * Created by gaetansenn on 28/07/2014.
 */

@RunWith(classOf[JUnitRunner])
class GithubOauth2Spec extends Specification {
  "GithubOauth" should {

    "UserHandler should throw an UserHandlerError with a bad Json format" in {
      val json = "{ \"emil\": \"test@hotmail.com\", \"name\": \"test\", \"login\": \"login\" }"
      GithubOauth2.UserHandler(json) must throwA[UserHandlerError]
    }

    "UserHandler with a good Json Format should return a good User Model" in {
      val json = "{ \"email\": \"test@hotmail.com\", \"name\": \"test\", \"login\": \"login\" }"
      val user = GithubOauth2.UserHandler(json)
      user.email must equalTo("test@hotmail.com")
      user.lastName must equalTo(Some("test"))
      user.username must equalTo("login")
    }
  }
}