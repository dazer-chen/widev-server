package db.migrations

import db.Migration
import models.Users
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 8/2/14.
 */
object M1_AddIndexesForUsersCollection extends Migration {
  override def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit] = {
    val users = Users(db)
    val indexes = users.collection.indexesManager

    Future.sequence(Seq(
      indexes.create(Index(List("username" -> IndexType.Text, "password" -> IndexType.Text), unique = true)),
      indexes.create(Index(List("email" -> IndexType.Text, "password" -> IndexType.Text), unique = true))
    )).mapTo[Unit]
  }
}