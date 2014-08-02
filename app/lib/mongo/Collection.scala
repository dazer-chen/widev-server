package lib.mongo

import reactivemongo.api.collections.default.BSONCollection

/**
 * Created by trupin on 7/27/14.
 */
trait Collection {
  val collection: BSONCollection
}