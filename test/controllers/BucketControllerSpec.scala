package controllers

import lib.{Util, WithFakeSessionApp}
import models._
import org.junit.runner.RunWith
import org.specs2._
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import reactivemongo.bson.BSONObjectID

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
  }

  "BucketController" should {

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
