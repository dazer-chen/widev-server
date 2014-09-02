package controllers

import lib.mongo.DuplicateModel
import models.{Workspace, Workspaces}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

/**
 * Created by thomastosoni on 8/31/14.
 */

class WorkspaceController(workspaces: Workspaces) extends Controller {
	self: AuthConfigImpl =>

	def getWorkspace(id: String) = Action.async {
		request =>
			workspaces.find(BSONObjectID(id)).map {
				case Some(workspace) => Ok(Json.toJson(workspace))
				case None => NotFound(s"Couldn't find workspace for id: $id")
			}
	}

	def createWorkspace(name: String, owner: BSONObjectID) = Action.async {
		request =>
			workspaces.create(Workspace(name, owner)).map {
				workspace => Ok(Json.toJson(workspace))
			} recover {
				case err: DuplicateModel =>
					NotAcceptable(s"Workspace already exists.")
			}
	}
}

object WorkspaceController extends WorkspaceController(Workspaces(ReactiveMongoPlugin.db)) with AuthConfigImpl
