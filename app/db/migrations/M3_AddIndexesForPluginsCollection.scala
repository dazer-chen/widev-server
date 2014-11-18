package db.migrations

import db.Migration
import models.Plugins
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{IndexType, Index}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by trupin on 11/8/14.
 */
object M3_AddIndexesForPluginsCollection extends Migration {
  override def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit] = {
    val plugins = Plugins(db)
    val indexes = plugins.collection.indexesManager

    Future.sequence(Seq(
      indexes.create(Index(List("name" -> IndexType.Ascending), unique = true)),
      indexes.create(Index(List("endPoint" -> IndexType.Ascending), unique = true))
    )).mapTo[Unit]
  }
}
