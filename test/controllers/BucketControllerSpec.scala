//package controllers
//
//import lib.{Util, WithFakeSessionApp}
//import managers.BucketManager
//import messages.MessageEnvelop
//import models._
//import org.junit.runner.RunWith
//import org.mockito.internal.matchers.Any
//import org.specs2._
//import org.specs2.mock.Mockito
//import org.specs2.runner.JUnitRunner
//import org.specs2.specification.Scope
//import play.api.http.HeaderNames
//import play.api.libs.iteratee.Input
//import play.api.libs.json.{JsArray, JsString, Json}
//import play.api.mvc.Result
//import play.api.test.Helpers._
//import reactivemongo.bson.BSONObjectID
//import scala.concurrent.Future
//
///**
// * Created by trupin on 10/20/14.
// */
//@RunWith(classOf[JUnitRunner])
//class BucketControllerSpec extends mutable.Specification with Mockito with Util {
//
//  trait MockFactory extends Scope {
//    self: WithFakeSessionApp =>
//
//    val bucketsMock = mock[Buckets]
//    val bucketManagerMock = mock[BucketManager]
//
//    val bucketController = new BucketController(bucketsMock) with AuthConfigMock
//    val customBucket = Bucket.generate
//    val customTeam = Team.generate
//  }
//
//  "BucketController" should {
//    ".addTeam" >> {
//      "add a team to a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
//        bucketsMock.addTeam(customBucket._id, customTeam._id) returns Future(true)
//        bucketsMock.find(customBucket._id) returns Future(Some(customBucket))
//
//        val body = Json.obj(
//          "team" -> JsString(customTeam._id.stringify)
//        ).toString()
//
//        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
//
//        val result: Future[Result] = bucketController.addTeam(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)
//
//        status(result) must equalTo(OK)
//      }
//    }
//
//    ".removeTeam" >> {
//      "remove a team to a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
//        bucketsMock.removeTeam(customBucket._id, customTeam._id) returns Future(true)
//        bucketsMock.find(customBucket._id) returns Future(Some(customBucket))
//
//        val body = Json.obj(
//          "team" -> JsString(customTeam._id.stringify)
//        ).toString()
//
//        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
//
//        val result: Future[Result] = bucketController.removeTeam(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)
//
//        status(result) must equalTo(OK)
//      }
//    }
//
//    ".updateTeam" >> {
//      "shouw update the team of a specific bucket" >> new WithFakeSessionApp(Standard) with MockFactory {
//        bucketsMock.updateTeams(customBucket._id, customBucket.teams.toSet) returns Future(true)
//
//        val body = Json.obj(
//          "teams" -> JsArray(customBucket.teams.map(team => JsString(team.stringify)).toSeq)
//        ).toString()
//
//        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
//
//        val result: Future[Result] = bucketController.updateTeams(customBucket._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)
//
//        status(result) must equalTo(OK)
//      }
//    }
//  }
//}
