package db.migrations

import db.Migration
import models.Buckets
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 11/8/14.
 */
object M4_AddIndexesForBucketsCollection extends Migration {
  override def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit] = {
    val buckets = Buckets(db)
    val indexes = buckets.collection.indexesManager

    Future.sequence(Seq(
      indexes.create(Index(List("name" -> IndexType.Ascending, "owner" -> IndexType.Ascending), unique = true))
    )).mapTo[Unit]
  }
}
