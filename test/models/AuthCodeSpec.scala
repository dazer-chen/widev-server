package models

import lib.mongo.Mongo
import org.joda.time.DateTime
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

/**
 * Created by trupin on 8/10/14.
 */
@RunWith(classOf[JUnitRunner])
class AuthCodeSpec extends Specification with Mongo with lib.Util {

  sequential

  val authCodes = factory.authCodes

  "AuthCodes" should {
    "relations" >> {
      authCodes.relations should equalTo(Seq(
        factory.users,
        factory.clients
      ))
    }

    ".find" >> {
      "valid by code" >> {
        "with expired auth code" >> {
          val expiredAuthCode = authCodes.generate.copy(expiresAt = DateTime.now.minusHours(1))
          result(authCodes.create(expiredAuthCode))
          result(authCodes.findValidByCode(expiredAuthCode.authorizationCode)) should beEmpty
        }
        "with valid auth code" >> {
          val validAuthCode = authCodes.generate
          result(authCodes.create(validAuthCode))
          result(authCodes.findValidByCode(validAuthCode.authorizationCode)) should equalTo(Some(validAuthCode))
        }
      }
    }
  }


}
