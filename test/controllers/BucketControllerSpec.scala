package controllers

import lib.mongo.DuplicateModel
import lib.{WithFakeSessionApp, Util}
import models._
import org.specs2.mock.Mockito
import org.specs2.mutable
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
* Created by thomastosoni on 8/31/14.
*/

class BucketControllerSpec extends mutable.Specification with Mockito with Util {

	trait MockFactory extends Scope {
		self: WithFakeSessionApp =>

    var currentBucket = Bucket.generate
		val userMock = mock[Users]
		val user = User.generate
		val bucketsMock = mock[Buckets]
		val bucketController = new BucketController(bucketsMock) with AuthConfigMock
	}

	"BucketController" should {
		".getBucket" >> {
			"should return a json Workspace model" >> new WithFakeSessionApp(Standard) with MockFactory {

				bucketsMock.find(any[BSONObjectID]) returns Future(Some(currentBucket))

				val result = bucketController.getBucket(currentBucket._id.stringify)(fakeRequest)

				contentType(result) must equalTo(Some("application/json"))

				contentAsJson(result) must beEqualTo(Json.toJson(currentBucket))

				there was one(bucketsMock).find(currentBucket._id)
			}

			"without a good id should return a bad access" >> new WithFakeSessionApp(Standard) with MockFactory {
				bucketsMock.find(any[BSONObjectID]) returns Future(None)

        val result = bucketController.getBucket(currentBucket._id.stringify)(fakeRequest)

				status(result) must equalTo(NOT_FOUND)

				there was one(bucketsMock).find(currentBucket._id)
			}

			"without an authenticated user should return an unauthorized error" >> new WithFakeSessionApp(Visitor) with MockFactory {

        val result = bucketController.getBucket(currentBucket._id.stringify)(fakeRequest)

				status(result) must equalTo(UNAUTHORIZED)
			}
		}

		".createBucket" >> {
			"should return the id of the bucket generated" >> new WithFakeSessionApp(Standard) with MockFactory {

        bucketsMock.create(any[Bucket]) returns Future(currentBucket)

				val result = bucketController.createBucket(currentBucket.name, currentBucket.owner)(fakeRequest)

				contentType(result) must equalTo(Some("application/json"))

				contentAsJson(result) must equalTo(Json.obj("id" -> currentBucket._id.stringify))

				there was one(bucketsMock).create(any[Bucket])
			}

			"with a duplicate bucket, should return an error" >> new WithFakeSessionApp(Standard) with MockFactory {
				bucketsMock.create(any[Bucket]) returns Future.failed(new DuplicateModel("Duplicate Bucket"))

				val result = bucketController.createBucket(currentBucket.name, currentBucket.owner)(fakeRequest)

				status(result) must equalTo(NOT_ACCEPTABLE)

				there was one(bucketsMock).create(any[Bucket])
			}

			"without an authenticated user should return an unauthorized error" >> new WithFakeSessionApp(Visitor) with MockFactory {

				val result = bucketController.createBucket(any[String], any[BSONObjectID])(fakeRequest)

				status(result) must equalTo(UNAUTHORIZED)
			}
		}
	}

}
