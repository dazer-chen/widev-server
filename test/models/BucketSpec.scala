package models

import lib.mongo.Mongo
import org.specs2.mutable.Specification
import reactivemongo.core.commands.LastError
import reactivemongo.bson.BSONObjectID

/**
* Created by thomastosoni on 8/31/14.
*/

class BucketSpec extends Specification with Mongo with lib.Util {

	sequential

	val buckets = factory.buckets

	"Bucket" should {
		".find" >> {
			val bucket = buckets.generate
			result(buckets.create(bucket))

			"with name and owner" >> {
				result(buckets.find(bucket.name, bucket.owner)).get should be equalTo bucket
			}
		}

		".delete" >> {
			"non existing workspace by workspace name" >> {
				val bucket = buckets.generate
				result(buckets.deleteByName(bucket.name)) must not be equalTo(LastError)
			}

			"existing workspace by workspace name" >> {
				val bucket = buckets.generate
				result(buckets.create(bucket))
				result(buckets.deleteByName(bucket.name)).n shouldEqual 1
			}
		}

    ".addTeam" >> {
      "already exists team in bucket" >> {
        val bucket = buckets.generate
        result(buckets.create(bucket))
        result(buckets.addTeam(bucket._id, bucket.teams.last)) must be equalTo(true)
        result(buckets.find(bucket._id)) should equalTo(Some(bucket))
      }

      "non existing team in bucket" >> {
        val bucket = buckets.generate
        val newTeam = BSONObjectID.generate
        result(buckets.create(bucket))
        result(buckets.addTeam(bucket._id, newTeam)) must be equalTo(true)
        result(buckets.find(bucket._id)) should equalTo(Some(bucket.copy(teams= bucket.teams+newTeam)))
      }
    }

    ".removeTeam" >> {
      "remove team if not exist" >> {
        val bucket = buckets.generate
        result(buckets.create(bucket))

        val lastTeam = bucket.teams.last
        result(buckets.removeTeam(bucket._id, lastTeam)) must be equalTo(true)
        result(buckets.find(bucket._id)) should equalTo(Some(bucket.copy(teams = bucket.teams-lastTeam)))
      }
    }
	}
}
