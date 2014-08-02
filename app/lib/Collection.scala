package lib

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection

/**
 * Created by trupin on 7/27/14.
 */
abstract class Collection(db: DefaultDB) {

  val collectionName: String

  val collection: BSONCollection = db.collection[BSONCollection](collectionName)
}