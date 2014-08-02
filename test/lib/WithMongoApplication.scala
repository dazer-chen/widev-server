package lib

import db.Migrations
import org.specs2.specification.{BeforeAfter, Scope}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoPlugin

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by trupin on 8/2/14.
 */
class WithMongoApplication extends Scope with BeforeAfter {
  implicit val app = FakeApplication()

  lazy val db = ReactiveMongoPlugin.db
  lazy val factory = CollectionFactory(db)

  def before =
    Await.result(Migrations.run(db), 10.second)
    play.api.Play.start(app)

  def after = db.drop.map {
    _ => play.api.Play.stop()
  }
}
