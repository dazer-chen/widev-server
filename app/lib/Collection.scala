package lib

import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.GenericCollection

/**
 * Created by trupin on 7/27/14.
 */
abstract class Collection[S <: GenericCollection](db: DefaultDB) {
  val collectionName: String

  val collection: S = db.collection[S](collectionName)
}