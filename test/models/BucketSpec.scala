package models

import lib.mongo.Mongo
import org.specs2.mutable.Specification
import reactivemongo.core.commands.LastError

/**
* Created by thomastosoni on 8/31/14.
*/

class BucketSpec extends Specification with Mongo with lib.Util {

	sequential

	val buckets = factory.buckets

	"Workspace" should {
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
	}
}
