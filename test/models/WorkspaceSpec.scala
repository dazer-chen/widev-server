package models

import lib.mongo.Mongo
import org.specs2.mutable.Specification
import reactivemongo.core.commands.LastError

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

			"with name and owner" >> {
				result(workspaces.find(workspace.name, workspace.owner)).get should be equalTo workspace
			}
		}

		".delete" >> {
			"non existing workspace by workspace name" >> {
				val workspace = workspaces.generate
				result(workspaces.deleteByName(workspace.name)) must not be equalTo(LastError)
			}

			"existing workspace by workspace name" >> {
				val workspace = workspaces.generate
				result(workspaces.create(workspace))
				result(workspaces.deleteByName(workspace.name)).n shouldEqual 1
			}
		}
	}
}
