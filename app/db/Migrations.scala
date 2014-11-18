package db

import org.joda.time.DateTime
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, Macros}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by trupin on 8/2/14.
 */
sealed case class AppliedMigration(index: Int, name: String, createdAt: DateTime = DateTime.now)

sealed class DbAlreadyUpToDate extends RuntimeException("The database is already up to date !")

object Migrations {
  import lib.util.Implicits._

  implicit val handler = Macros.handler[AppliedMigration]
  val migrations = ReactiveMongoPlugin.db.collection[BSONCollection]("migrations")

  def runOnFirstApplicationStart(): Future[Unit] = {
    migrations.find(BSONDocument()).one[AppliedMigration].map {
      case None => run()
      case _ => Unit
    }
  }

  def run(): Future[Seq[AppliedMigration]] = {
    migrations.find(BSONDocument()).sort(BSONDocument("index" -> -1)).one[AppliedMigration].flatMap {
      case Some(lastMigration) if lastMigration.index + 1 >= MigrationRecords.migrations.size =>
        throw new DbAlreadyUpToDate
      case Some(lastMigration) =>
        runMigrations(lastMigration.index + 1, MigrationRecords.migrations.splitAt(lastMigration.index + 1)._2)
      case None =>
        runMigrations(0, MigrationRecords.migrations)
    }.flatMap(_.find(BSONDocument()).sort(BSONDocument("$natural" -> true)).cursor[AppliedMigration].collect[Seq]())
  }

  private def runMigrations(index: Int, ms: Seq[Migration]) =
    ms.foldLeft((Future {}, 0)) {
      (t, obj) =>
        (t._1.flatMap {
          _ =>
            migrations.insert(AppliedMigration(t._2 + index, obj.toString)).map {
              _ => obj.run(ReactiveMongoPlugin.db)
            }
        }, t._2 + 1)
    }._1.map { _ => migrations }
}
