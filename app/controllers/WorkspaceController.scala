package controllers

import jp.t2v.lab.play2.auth.AuthElement
import lib.mongo.DuplicateModel
import models.{Standard, Workspace, Workspaces}
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

/**
 * Created by thomastosoni on 8/31/14.
 */

class WorkspaceController(workspaces: Workspaces) extends Controller with AuthElement {
	self: AuthConfigImpl =>

	def getWorkspace(id: String) = AsyncStack(AuthorityKey -> Standard) {
		request =>
			workspaces.find(BSONObjectID(id)).map {
				case Some(workspace) => Ok(Json.toJson(workspace))
				case None => NotFound(s"Couldn't find workspace for id: $id")
			}
	}

	def createWorkspace(name: String, admin_name: String) =  AsyncStack(AuthorityKey -> Standard) {
		request =>
			workspaces.create(Workspace(name, admin_name)).map {
				workspace => Ok(Json.toJson(workspace))
			} recover {
				case err: DuplicateModel =>
					NotAcceptable(s"Workspace already exists.")
			}
	}
}

object WorkspaceController extends WorkspaceController(Workspaces(ReactiveMongoPlugin.db)) with AuthConfigImpl
