package controllers

import lib.{Util, WithFakeSessionApp}
import models._
import org.junit.runner.RunWith
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
    val s3BucketMock = mock[fly.play.s3.Bucket]
    val bucketController = new BucketController(bucketsMock, s3BucketMock) with AuthConfigMock
    val customBucket = Bucket.generate
  }

  "BucketController" should {

    ".updateTeam" >> {
      "shouw update the team of a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
        bucketsMock.updateTeam(customBucket._id, customBucket.teams.toSet) returns Future(true)

        val body = Json.obj(
          "id" -> JsString(customBucket._id.stringify),
          "teams" -> JsArray(customBucket.teams.map(team => JsString(team.stringify)).toSeq)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = bucketController.updateTeam.apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

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

        val bytes = BSONObjectID.generate.stringify.getBytes

        val bytesResult = bucketController.writeMessage(insertMessage, Some(bytes))

        (bytesResult should not).beNone

        val messageResult = bucketController.readMessage(bytesResult.get)

        (messageResult should not).beNone

        messageResult.get.bytes should beSome(bytes)
        messageResult.get.message should be equalTo insertMessage
      }
    }

  }

}
