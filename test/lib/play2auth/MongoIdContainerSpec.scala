package lib.play2auth

import lib.mongo.Mongo
import models.{Sessions, Session}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent.Future

/**
 * Created by gaetansenn on 15/08/2014.
 */


/**
 * Created by gaetansenn on 10/08/2014.
 */
@RunWith(classOf[JUnitRunner])
class MongoIdContainerSpec extends Specification with Mongo with lib.Util with Mockito {

//  "MongoIdContainer" should {
//
//    val sessionMock = mock[Sessions]
//
//    "get Id from token" >> {
//      val session = sessionMock.generate
//      when(sessionMock.findByToken(any[String])).thenReturn(Future(Some(session)))
//      val mongoIdContainer = new MongoIdContainer[String](sessionMock)
//      mongoIdContainer.get(session.token) should be(session._id.stringify)
//    }
//
//  }
}
