package lib.mongo

import db.Migrations
import models.Factory
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.api.MongoDriver

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by trupin on 8/2/14.
 */
trait Mongo {
  lazy val driver = new MongoDriver
  lazy val connection = driver.connection(List("localhost:27017"))

  val timeout = 10.seconds

  val db = connection("spec2-test-widev")

  Await.ready(db.drop, timeout)
//  Await.ready(Migrations.run(), timeout)

  lazy val factory = new Factory(db)
}
