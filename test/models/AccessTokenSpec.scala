package models

import lib.mongo.Mongo
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 8/10/14.
 */
@RunWith(classOf[JUnitRunner])
class AccessTokenSpec extends Specification with Mongo with lib.Util {

  sequential

  val accessTokens = factory.accessTokens

  "AccessTokens" should {
    "relations" >> {
      accessTokens.relations should equalTo(Seq(
        factory.users,
        factory.clients
      ))
    }

    "find" >> {
      val accessToken = accessTokens.generate
      result(accessTokens.create(accessToken))

      "with userId and clientId" >> {
        result(accessTokens.find(accessToken.userId, accessToken.clientId)) should equalTo(Some(accessToken))
      }
      "with clientId and scope" >> {
        "scope is Some" >> {
          result(accessTokens.find(accessToken.clientId, accessToken.scope)) should equalTo(Some(accessToken))
        }
        "scope is None" >> {
          "in search only" >> {
            result(accessTokens.find(accessToken.clientId, None)) should beEmpty
          }
          "in search and model" >> {
            val accessToken = accessTokens.generate.copy(scope = None)
            result(accessTokens.create(accessToken))
            result(accessTokens.find(accessToken.clientId, None)) should equalTo(Some(accessToken))
          }
        }
      }
      "by refreshToken" >> {
        result(accessTokens.findByRefreshToken(accessToken.refreshToken.get)) should equalTo(Some(accessToken))
      }
      "by accessToken" >> {
        result(accessTokens.findByAccessToken(accessToken.accessToken)) should equalTo(Some(accessToken))
      }
    }

    "deleteExistingAndCreate" >> {
      "with an existing model" >> {
        val accessToken = accessTokens.generate
        val accessToken1 = accessTokens.generate.copy(userId = accessToken.userId, clientId = accessToken.clientId)
        val accessToken2 = accessTokens.generate.copy(userId = accessToken.userId, clientId = accessToken.clientId)

        result(accessTokens.create(accessToken1))
        result(accessTokens.create(accessToken2))

        result(accessTokens.deleteExistingAndCreate(accessToken))
        result(accessTokens.find(accessToken._id)) should equalTo(Some(accessToken))
        result(accessTokens.find(accessToken1._id)) should beEmpty
        result(accessTokens.find(accessToken2._id)) should beEmpty
      }
      "with a non existing model" >> {
        val accessToken = accessTokens.generate
        result(accessTokens.deleteExistingAndCreate(accessToken))
        result(accessTokens.find(accessToken._id)).get should equalTo(accessToken)
      }
    }
  }
}
