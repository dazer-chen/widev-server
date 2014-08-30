package db

import migrations._
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson.{BSONDocument, Macros}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by trupin on 8/2/14.
 */
sealed case class AppliedMigration(index: Int, name: String, createdAt: DateTime = DateTime.now)

sealed class DbAlreadyUpToDate extends RuntimeException("The database is already up to date !")

object Migrations {
  import lib.util.Implicits._

  implicit val handler = Macros.handler[AppliedMigration]

  def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Seq[AppliedMigration]] = {
    implicit val migrations = db.collection[BSONCollection]("migrations")

    migrations.indexesManager.ensure(Index(List("created_at" -> IndexType.Descending)))

    migrations.find(BSONDocument()).sort(BSONDocument("$natural" -> true)).one[AppliedMigration].flatMap {
      case Some(lastMigration) if lastMigration.createdAt.isBefore(DateTime.now) =>
        throw new DbAlreadyUpToDate
      case Some(lastMigration) =>
        runMigrations(MigrationRecords.migrations.splitAt(lastMigration.index)._2, db)
      case None =>
        runMigrations(MigrationRecords.migrations, db)
    }.flatMap(_.find(BSONDocument()).sort(BSONDocument("$natural" -> true)).cursor[AppliedMigration].collect[Seq]())
  }

  private def runMigrations(migrations: Seq[Migration], db: DefaultDB)(implicit collection: BSONCollection, ec: ExecutionContext) =
    migrations.foldLeft((Future(), 0)) {
      (t, obj) =>
        (t._1.flatMap {
          _ =>
            collection.insert(AppliedMigration(t._2, obj.toString)).map {
              _ => obj.run(db)
            }
        }, t._2)
    }._1.map { _ => collection }
}
