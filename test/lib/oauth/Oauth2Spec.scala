package lib.oauth

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.ws.WSResponse
import play.api.test.WithApplication

/**
 * Created by gaetansenn on 29/07/2014.
 */

@RunWith(classOf[JUnitRunner])
class RegisterSpec extends Specification {
  "Oauth2" should {

    "Return a valid route url for the signing" in new WithApplication {
      new {
        val clientId = "clientIdTest"
        val clientSecret = "clientSecretTest"
        val signInUrl = "http://oauth.test"
        val accessTokenUrl = "http://accesstoken.test"
        val redirectUrl = "/oauth/redirect"
        val scope = "test,test2"
        val responseType = ""
        val grantType = ""
      } with Oauth2 {
        override def token(resp: WSResponse): String = ???

        signIn() must equalTo("http://oauth.test?client_id=clientIdTest&redirect_uri=/oauth/redirect&scope=test,test2")
      }
    }
  }
}