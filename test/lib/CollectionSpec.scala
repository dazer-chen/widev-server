package lib

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONObjectID, Macros}

/**
 * Created by trupin on 8/3/14.
 */
@RunWith(classOf[JUnitRunner])
class CollectionSpec extends Specification with Mongo with Util {

  case class Model1(_id: BSONObjectID = BSONObjectID.generate, model2Id: BSONObjectID)
  object Model1 {
    implicit val handler = Macros.handler[Model1]
  }

  class Model1s(model2s: => Model2s) extends lib.mongo.Collection[Model1] {
    override val collection: BSONCollection = db[BSONCollection]("model1s")

    override def relations = Seq(model2s)

    override def generate = Model1(model2Id = BSONObjectID.generate)
  }

  case class Model2(_id: BSONObjectID = BSONObjectID.generate, model1Id: BSONObjectID)
  object Model2 {
    implicit val handler = Macros.handler[Model2]
  }

  class Model2s(model1s: => Model1s) extends lib.mongo.Collection[Model2] {
    override val collection: BSONCollection = db[BSONCollection]("model2s")

    override def relations = Seq(model1s)

    override def generate = Model2(model1Id = BSONObjectID.generate)
  }

  lazy val model1s: Model1s = new Model1s(model2s)
  lazy val model2s: Model2s = new Model2s(model1s)

  sequential

  "Collection" should {
    ".create" >> {
      val model = model1s.generate
      result(model1s.create(model)) should equalTo(model)
    }
  }

}
