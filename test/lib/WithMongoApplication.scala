package lib

import org.specs2.execute.{AsResult, Result}
import org.specs2.specification.{After, Scope}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoPlugin

/**
 * Created by trupin on 8/2/14.
 */
object WithMongoApplication {
  var baseIndex = 0
}

class WithMongoApplication(mongoUri: String = "mongodb://localhost:27017") extends Scope with After {
  import lib.WithMongoApplication._

  implicit val app = FakeApplication(additionalConfiguration = Map("mongodb.uri" -> s"$mongoUri/widev-test-$baseIndex"))
  baseIndex += 1

  lazy val db = ReactiveMongoPlugin.db
  lazy val factory = CollectionFactory(db)

  play.api.Play.start(app)

  def after =
    db.drop.map {
      _ => play.api.Play.stop()
    }
}
