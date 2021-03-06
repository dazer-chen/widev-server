package controllers

import play.api.http.HeaderNames

import scala.concurrent.Future

import lib.{FakeSession, Util, WithFakeSessionApp}
import models.{User, Standard, Team, Teams}
import org.junit.runner.RunWith
import org.specs2.mutable
import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import play.api.libs.iteratee.Input
import play.api.libs.json.{JsObject, JsArray, JsString, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import reactivemongo.bson.BSONObjectID

/**
 * Created by gaetansenn on 24/10/14.
 */
@RunWith(classOf[JUnitRunner])
class TeamControllerSpec extends mutable.Specification with Mockito with Util {
  trait MockFactory extends Scope {
    self: WithFakeSessionApp =>

    val teamsMock = mock[Teams]
    val teamController = new TeamController(teamsMock) with AuthConfigMock
    val currentTeam = Team.generate
    val userTeam = User.generate
  }

  "TeamController" should {

    ".getTeam" >> {
      "should return a valid team if authentificated and exist" >> new WithFakeSessionApp(Standard) with MockFactory {
        teamsMock.find(any[BSONObjectID]) returns Future(Some(currentTeam))

        val body = ""

        val request = fakeRequest.withBody(body)

        val result: Future[Result] = teamController.getTeam(currentTeam._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        contentType(result) must equalTo(Some("application/json"))

        contentAsJson(result) must beEqualTo(Json.toJson(currentTeam))

      }

      "should return a notFound request if no team found" >> new WithFakeSessionApp(Standard) with MockFactory {
        teamsMock.find(any[BSONObjectID]) returns Future(None)

        val body = ""

        val request = fakeRequest.withBody(body)

        val result: Future[Result] = teamController.getTeam(currentTeam._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(NOT_FOUND)
      }
    }

    ".addUser" >> {
      "add an user to a specific team" >> new WithFakeSessionApp(Standard) with MockFactory {
        teamsMock.addUser(currentTeam._id, userTeam._id) returns Future(true)

        val body = Json.obj(
          "user" -> JsString(userTeam._id.stringify)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = teamController.addUser(currentTeam._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(OK)
      }
    }

    ".removeUser" >> {
      "remove an user to a specific team" >> new WithFakeSessionApp(Standard) with MockFactory {
        teamsMock.removeUser(currentTeam._id, userTeam._id) returns Future(true)

        val body = Json.obj(
          "user" -> JsString(userTeam._id.stringify)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = teamController.removeUser(currentTeam._id.stringify).apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        status(result) must equalTo(OK)
      }
    }

    ".getTeams" >> {
      "should return a valid list of team if authentificated" >> new WithFakeSessionApp(Standard) with MockFactory {
        teamsMock.findByOwner(currentUser._id) returns Future(List(currentTeam))

        val body = ""

        val request = fakeRequest.withBody(body)

        val result: Future[Result] = teamController.getTeams.apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        contentType(result) must equalTo(Some("application/json"))

        contentAsJson(result) must beEqualTo(Json.toJson(List(currentTeam)))
      }
    }

    ".createTeam" >> {
      "create a team with some user should return the created team" >> new WithFakeSessionApp(Standard) with MockFactory {
        val customTeam = Team(currentTeam.name, currentUser._id, currentTeam.users)

        teamsMock.create(any[Team]) returns Future(customTeam)

        val body = Json.obj(
          "name" -> JsString(customTeam.name),
          "users" -> JsArray(customTeam.users.map(user => JsString(user.stringify)).toSeq)
        ).toString()

        val request = fakeRequest.withBody(body).withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")

        val result: Future[Result] = teamController.createTeam.apply(request).feed(Input.El(body.getBytes)).flatMap(_.run)

        contentType(result) must equalTo(Some("application/json"))

        contentAsJson(result) must beEqualTo(Json.toJson(customTeam))
      }
    }
  }
}