package models

import lib.mongo.Mongo
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import reactivemongo.core.commands.LastError

import scala.concurrent.Await

/**
 * Created by gaetansenn on 10/08/2014.
 */
@RunWith(classOf[JUnitRunner])
class SessionSpec extends Specification with Mongo with lib.Util {

  sequential

  val sessions = factory.session

  "Session" should {

    "find" >> {
      val session = sessions.generate
      result(sessions.create(session))

      "with a token" >> {
        result(sessions.findByToken(session.token)) should equalTo(Some(session))
      }
    }

    "update" >> {
      val session = sessions.generate

      "refreshToken without session in database" >> {
        result[Option[Session]](sessions.refreshToken(session.token, 0)) should none
      }

      "refreshToken with session in database" >> {
        result(sessions.create(session))
        result[Option[Session]](sessions.refreshToken(session.token, 20)) should beSome[Session]
        result[Option[Session]](sessions.findByToken(session.token)).get.createdAt.getMillis must beGreaterThan(session.createdAt.getMillis)
      }

    }

    "remove" >> {

      "remove a non existing session by token" >> {
        val session = sessions.generate
        result[LastError](sessions.removeByToken(session.token)).n shouldEqual(0)
      }

      "remove a session from an specific token" >> {
        val session = sessions.generate
        result(sessions.create(session))
        result[LastError](sessions.removeByToken(session.token)).n shouldEqual(1)
      }

      "remove a non existing session by userId" >> {
        val session = sessions.generate
        result[LastError](sessions.removeByUser(session.userId)).n shouldEqual(0)
      }

      "remove a session from a specific userId" >> {
        val session = sessions.generate
        result(sessions.create(session))
        result[LastError](sessions.removeByUser(session.userId)).n shouldEqual(1)
      }

    }

    "util" >> {

      "expired token with specific timeout should be cleaned properly " >> {
        val session = sessions.generate.copy(createdAt = DateTime.now.minusSeconds(2) )
        result(sessions.create(session))
        result(sessions.cleanExpiredTokens(Some(1)))
        result[Option[Session]](sessions.findByToken(session.token)) should none
      }
    }


  }
}
