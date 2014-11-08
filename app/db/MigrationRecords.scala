package db

import db.migrations._

/**
 * Created by trupin on 8/30/14.
 */

/**
 * To be applied, a migration must be recorded here !
 */
object MigrationRecords {
  val migrations = Seq(
    M1_AddIndexesForUsersCollection,
    M2_AddDefaultUsers,
    M3_AddIndexesForPluginsCollection,
    M4_AddIndexesForBucketsCollection
  )
}
