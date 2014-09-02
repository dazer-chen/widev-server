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

class WorkspaceControllerSpec extends mutable.Specification with Mockito with Util {

	trait MockFactory extends Scope {
		self: WithFakeSessionApp =>

		val workspacesMock = mock[Workspaces]
		val workspaceController = new WorkspaceController(workspacesMock) with AuthConfigMock
	}

	"WorkspaceController" should {
		".getWorkspace" >> {
			"should return a json Workspace model" >> new WithFakeSessionApp(Standard) with MockFactory {
				workspacesMock.find(any[BSONObjectID]) returns Future(Some(currentWorkspace))

				val result = workspaceController.getWorkspace(currentWorkspace._id.stringify)(fakeRequest)

				contentType(result) must equalTo(Some("application/json"))

				contentAsJson(result) must beEqualTo(Json.toJson(currentWorkspace))

				there was one(workspacesMock).find(currentWorkspace._id)
			}

			"without a good id should return a bad access" >> new WithFakeSessionApp(Standard) with MockFactory {
				workspacesMock.find(any[BSONObjectID]) returns Future(None)

				val result = workspaceController.getWorkspace(currentWorkspace._id.stringify)(fakeRequest)

				status(result) must equalTo(NOT_FOUND)

				there was one(workspacesMock).find(currentWorkspace._id)
			}

			"without an authenticated user should return an unauthorized error" >> new WithFakeSessionApp(Visitor) with MockFactory {
				val result = workspaceController.getWorkspace(currentWorkspace._id.stringify)(fakeRequest)

				status(result) must equalTo(UNAUTHORIZED)
			}
		}

		".createWorkspace" >> {
			val fakeWorkspace = Workspace.generate
			"should return a json Workspace" >> new WithFakeSessionApp(Standard) with MockFactory {
				workspacesMock.create(any[Workspace]) returns Future(fakeWorkspace)

				val result = workspaceController.createWorkspace(fakeWorkspace.name, fakeWorkspace.owner)(fakeRequest)

				contentType(result) must equalTo(Some("application/json"))

				contentAsJson(result) must equalTo(Json.toJson(fakeWorkspace))

				there was one(workspacesMock).create(any[Workspace])
			}

			"with a duplicate workspace, should return an error" >> new WithFakeSessionApp(Standard) with MockFactory {
				workspacesMock.create(any[Workspace]) returns Future.failed(new DuplicateModel("Duplicate workspace"))

				val result = workspaceController.createWorkspace(fakeWorkspace.name, fakeWorkspace.owner)(fakeRequest)

				status(result) must equalTo(NOT_ACCEPTABLE)

				there was one(workspacesMock).create(any[Workspace])
			}

			"without an authenticated user should return an unauthorized error" >> new WithFakeSessionApp(Visitor) with MockFactory {
				val result = workspaceController.createWorkspace(fakeWorkspace.name, fakeWorkspace.owner)(fakeRequest)

				status(result) must equalTo(UNAUTHORIZED)
			}
		}
	}

}
