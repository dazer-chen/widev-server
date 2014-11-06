package controllers

import lib.{Util, WithFakeSessionApp}
import managers.BucketManager
import messages.MessageEnvelop
import models._
import org.junit.runner.RunWith
import org.mockito.internal.matchers.Any
import org.specs2._
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.http.HeaderNames
import play.api.libs.iteratee.Input
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID
import scala.concurrent.Future

/**
 * Created by trupin on 10/20/14.
 */
@RunWith(classOf[JUnitRunner])
class BucketControllerSpec extends mutable.Specification with Mockito with Util {

  trait MockFactory extends Scope {
    self: WithFakeSessionApp =>

    val bucketsMock = mock[Buckets]
    val bucketManagerMock = mock[BucketManager]
    val s3BucketMock = mock[fly.play.s3.Bucket]

    val bucketController = new BucketController(bucketsMock, s3BucketMock) with AuthConfigMock
    val customBucket = Bucket.generate
    val customTeam = Team.generate
  }

  "BucketController" should {

    ".addTeam" >> {
      "add a team to a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
        bucketsMock.addTeam(customBucket._id, customTeam._id) returns Future(true)
        bucketsMock.find(customBucket._id) returns Future(Some(customBucket))

        val body = Json.obj(
        "team" -> JsString(customTeam._id.stringify)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = bucketController.addTeam(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(OK)
      }
    }

    ".removeTeam" >> {
      "remove a team to a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
        bucketsMock.removeTeam(customBucket._id, customTeam._id) returns Future(true)
        bucketsMock.find(customBucket._id) returns Future(Some(customBucket))

        val body = Json.obj(
          "team" -> JsString(customTeam._id.stringify)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = bucketController.removeTeam(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(OK)
      }
    }

    ".updateTeam" >> {
      "shouw update the team of a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
        bucketsMock.updateTeams(customBucket._id, customBucket.teams.toSet) returns Future(true)

        val body = Json.obj(
          "teams" -> JsArray(customBucket.teams.map(team => JsString(team.stringify)).toSeq)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = bucketController.updateTeams(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(OK)
      }
    }

    ".writeMessage & .readMessage" >> {
      "should write a message then parse it correctly" >> new WithFakeSessionApp(Standard) with MockFactory {
        import messages._

        val insertMessage = InsertFileAction(
          fd = BSONObjectID.generate.stringify,
          sessionToken = Some(BSONObjectID.generate.stringify),
          at = 2
        )

//    ".writeMessage & .readMessage" >> {
//      "should write a message then parse it correctly" >> new WithFakeSessionApp(Standard) with MockFactory {
//        import messages._
//
//        val insertMessage = InsertFileAction(
//          fd = BSONObjectID.generate.stringify,
//          sessionToken = Some(BSONObjectID.generate.stringify),
//          at = 2
//        )
//
//        val bytes = BSONObjectID.generate.stringify.getBytes
//
//        val bytesResult = bucketControllerSpy.writeMessage(insertMessage, Some(bytes))
//
//        (bytesResult should not).beNone
//
//        val messageResult = bucketControllerSpy.readMessage(bytesResult.get)
//
//        (messageResult should not).beNone
//
//        messageResult.get.bytes should beSome(bytes)
//        messageResult.get.message should be equalTo insertMessage
      }
    }
//
//    ".receiveMessage" >> {
//      "should receive a message execute its associated action" >> new WithFakeSessionApp(Standard) with MockFactory {
//        import messages._
//
//        val insertMessageMock = mock[FileAction]
//        bucketManagerMock.readMessage(any[Array[Byte]]) returns Some(new MessageEnvelop(insertMessageMock, None))
//        insertMessageMock.action(any[BSONObjectID], any[Option[Array[Byte]]], any[((FileAction, Option[Array[Byte]]) => Unit)])
//
//        val bytes = Array[Byte]()
//        bucketController.receiveMessage(bytes)
//
//        there was one(bucketManagerMock).readMessage(any[Array[Byte]])
//        there was one(insertMessageMock).action(currentUser._id, Some(bytes), any[((FileAction, Option[Array[Byte]]) => Unit)])
//      }
//    }

//    ".broadCastMessageToFileRoom" >> {
//      "should receive a message and send it to every clients who opened the same file" >> new WithFakeSessionApp(Standard) with MockFactory {
//        import messages._
//
//        val insertMessageSpy = spy(InsertFileAction(
//          fd = BSONObjectID.generate.stringify,
//          sessionToken = Some(bucketControllerSpy.idContainer.startNewSession(currentUser._id.stringify, 10)),
//          at = 2
//        ))
//        val bytes = BSONObjectID.generate.stringify.getBytes
//        val packetBytes = bucketControllerSpy.writeMessage(insertMessageSpy, Some(bytes))
//
//      }
//    }

  }

}
