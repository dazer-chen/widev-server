package models

import lib.mongo.Mongo
import org.specs2.mutable.Specification
import reactivemongo.bson.BSONObjectID

/**
 * Created by gaetansenn on 06/11/14.
 */

class TeamSpec extends Specification with Mongo with lib.Util {

  sequential

  val teams = factory.teams
  val users = factory.users

  "Team" should {
    ".addUser" >> {
      "already exists user in team" >> {
        val team = teams.generate
        val lastUser = team.users.last
        result(teams.create(team))
        result(teams.addUser(team._id, lastUser)) must be equalTo(true)
        result(teams.find(team._id)) should equalTo(Some(team))
      }

      "non existing user in team" >> {
        val team = teams.generate
        val newUser = BSONObjectID.generate
        result(teams.create(team))
        result(teams.addUser(team._id, newUser)) must be equalTo(true)
        result(teams.find(team._id)) should equalTo(Some(team.copy(users = team.users+newUser)))
      }
    }

    ".removeUser" >> {
      "remove last user in team" >> {
        val team = teams.generate
        result(teams.create(team))

        val lastUser = team.users.last
        result(teams.removeUser(team._id, lastUser)) must be equalTo(true)
        result(teams.find(team._id)) should equalTo(Some(team.copy(users = team.users-lastUser)))
      }
    }
  }
}
