package db.migrations

import db.Migration
import models.{Teams, Buckets}
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 11/8/14.
 */
object M5_AddIndexesForTeamsCollection extends Migration {
  override def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit] = {
    val teams = Teams(db)
    val indexes = teams.collection.indexesManager

    Future.sequence(Seq(
      indexes.create(Index(List("name" -> IndexType.Ascending, "owner" -> IndexType.Ascending), unique = true))
    )).mapTo[Unit]
  }
}