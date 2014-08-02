package lib

import org.specs2.specification.{BeforeAfter, Scope}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

/**
 * Created by trupin on 8/2/14.
 */
class WithMongoApplication(mongoUri: String = "mongodb://localhost:27017") extends Scope with BeforeAfter {
  implicit val app = FakeApplication(additionalConfiguration = Map("mongodb.uri" -> s"$mongoUri/widev-test-${BSONObjectID.generate.stringify}"))

  lazy val db = ReactiveMongoPlugin.db
  lazy val factory = CollectionFactory(db)

  def before = play.api.Play.start(app)

  def after = db.drop.map {
    _ => play.api.Play.stop()
  }
}
