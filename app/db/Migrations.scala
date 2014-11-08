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


    migrations.find(BSONDocument()).sort(BSONDocument("index" -> -1)).one[AppliedMigration].flatMap {
      case Some(lastMigration) if lastMigration.index + 1 >= MigrationRecords.migrations.size =>
        throw new DbAlreadyUpToDate
      case Some(lastMigration) =>
        runMigrations(lastMigration.index + 1, MigrationRecords.migrations.splitAt(lastMigration.index + 1)._2, db)
      case None =>
        runMigrations(0, MigrationRecords.migrations, db)
    }.flatMap(_.find(BSONDocument()).sort(BSONDocument("$natural" -> true)).cursor[AppliedMigration].collect[Seq]())
  }

  private def runMigrations(index: Int, migrations: Seq[Migration], db: DefaultDB)(implicit collection: BSONCollection, ec: ExecutionContext) =
    migrations.foldLeft((Future {}, 0)) {
      (t, obj) =>
        (t._1.flatMap {
          _ =>
            collection.insert(AppliedMigration(t._2 + index, obj.toString)).map {
              _ => obj.run(db)
            }
        }, t._2 + 1)
    }._1.map { _ => collection }
}
