package lib.play2auth

import lib.WithFakeApp
import models.{Session, Sessions}
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import reactivemongo.core.commands.LastError

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by gaetansenn on 15/08/2014.
 */


/**
 * Created by gaetansenn on 10/08/2014.
 */
@RunWith(classOf[JUnitRunner])
class MongoIdContainerSpec extends Specification with lib.Util with Mockito {

  "MongoIdContainer" should {
    val session = Session.generate

    "get Id(ClassTag) from token" >> new WithFakeApp {
      val sessionMock = mock[Sessions]

      val mongoIdContainer = new MongoIdContainer[String](sessionMock)

      when(sessionMock.findByToken(session.token)).thenReturn(Future(Some(session)))
      mongoIdContainer.get(session.token) should equalTo(Some(session.userId.stringify))
    }

    "create a new session " >> new WithFakeApp {
      val sessionMock = mock[Sessions]

      val mongoIdContainer = new MongoIdContainer[String](sessionMock)

      when(sessionMock.create(any[Session])).thenReturn(Future(session))
      when(sessionMock.removeByUser(session.userId)).thenReturn(any[Future[LastError]])

      mongoIdContainer.startNewSession(session.userId.stringify, 0) must equalTo(session.token)
    }

  }
}
