package models

import lib.mongo.Mongo
import org.specs2.mutable.Specification

/**
 * Created by thomastosoni on 8/31/14.
 */

class WorkspaceSpec extends Specification with Mongo with lib.Util {

	sequential

	val workspaces = factory.workspaces

	"Workspace" should {

		".find" >> {
			val workspace = workspaces.generate
			result(workspaces.create(workspace))

			"with name and administrator" >> {
				result(workspaces.find(workspace.name, workspace.admin)).get should equalTo(workspace)
			}
		}
	}
}
