package lib.mongo

import lib.Util
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

/**
 * Created by trupin on 8/3/14.
 */
@RunWith(classOf[JUnitRunner])
class CollectionSpec extends Specification with Mongo with Util {

  case class Model1(_id: BSONObjectID = BSONObjectID.generate, model2Id: BSONObjectID)
  object Model1 {
    implicit val handler = Macros.handler[Model1]
  }

  class Model1s(model2s: => Model2s) extends Collection[Model1] {
    override val collection: BSONCollection = db[BSONCollection]("model1s")

    override def relations = Seq(model2s)

    override def generate = Model1(model2Id = BSONObjectID.generate)
  }

  case class Model2(_id: BSONObjectID = BSONObjectID.generate, model1Id: BSONObjectID)
  object Model2 {
    implicit val handler = Macros.handler[Model2]
  }

  class Model2s(model1s: => Model1s) extends Collection[Model2] {
    override val collection: BSONCollection = db[BSONCollection]("model2s")

    override def relations = Seq(model1s)

    override def generate = Model2(model1Id = BSONObjectID.generate)
  }

  lazy val model1s: Model1s = new Model1s(model2s)
  lazy val model2s: Model2s = new Model2s(model1s)

  sequential

  "Collection" should {
    "SuperClass" >> {
      ".exists" >> {
        val model = model1s.generate

        result(model1s.collection.insert(model))
        "with field and value" >> {
          result(model1s.exists("_id", model._id)) should beEqualTo(true)
          result(model1s.exists("_id", BSONObjectID.generate)) should beEqualTo(false)
        }
        "with id" >> {
          result(model1s.exists(model._id)) should beEqualTo(true)
          result(model1s.exists(BSONObjectID.generate)) should beEqualTo(false)
        }
      }

      ".delete" >> {
        val model1 = model1s.generate
        result(model1s.collection.insert(model1))

        result(model1s.delete(model1._id)) should beEqualTo(true)
        result(model1s.delete(BSONObjectID.generate)) should beEqualTo(false)
      }

      ".deepDelete" >> {
        "success to deeply delete a model" >> {
          val model1 = model1s.generate
          val model2 = model2s.generate.copy(_id = model1.model2Id)

          result(model1s.collection.insert(model1))
          result(model2s.collection.insert(model2))

          result(model1s.deepDelete(model1._id)) must beEqualTo(true)
          result(model2s.collection.find(BSONDocument("_id" -> model2._id)).one[Model2]) must beEmpty
          result(model1s.collection.find(BSONDocument("_id" -> model1._id)).one[Model1]) must beEmpty
        }

        "return false after deleting a corrupted model" >> {
          val model1 = model1s.generate

          result(model1s.collection.insert(model1))

          result(model1s.deepDelete(model1._id)) must beEqualTo(false)
          result(model1s.collection.find(BSONDocument("_id" -> model1._id)).one[Model1]) must beEmpty
        }
      }
    }
  }

  "BaseClass" >> {
    ".deepCreate" >> {
      val model = model1s.generate
      result(model1s.deepCreate(model)) should equalTo(model)

      result(model2s.collection.find(BSONDocument("_id" -> model.model2Id)).one[Model2]) should not beEmpty
    }

    ".safeCreate" >> {
      val model = model1s.generate
      "fails with corrupted model" >> {
        result(model1s.safeCreate(model)) should equalTo(false)
        result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]) should beEmpty
      }
      "success with valid model" >> {
        val model2 = model2s.generate.copy(_id = model.model2Id)
        result(model2s.collection.insert(model2))

        result(model1s.safeCreate(model)) should equalTo(true)
        result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get should equalTo(model)
      }
    }

    ".find" >> {
      val model = model1s.generate
      result(model1s.collection.insert(model))
      result(model1s.find(model._id)).get should equalTo(model)
      result(model1s.find(BSONObjectID.generate)) should beEmpty
    }

    ".create" >> {
      val model = model1s.generate
      result(model1s.create(model)) should equalTo(model)
      result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get must equalTo(model)
    }

    ".save" >> {
      val model = model1s.generate
      result(model1s.save(model)) should equalTo(model)
      result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get must equalTo(model)

      val updatedModel = model.copy(model2Id = BSONObjectID.generate)
      result(model1s.save(updatedModel)) should equalTo(updatedModel)
      result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get must equalTo(updatedModel)
    }

    ".safeSave" >> {
      val model = model1s.generate
      "fails with corrupted model" >> {
        result(model1s.safeSave(model)) should beEqualTo(false)
        result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]) must beEmpty
      }
      "success with valid model" >> {
        val model2 = model2s.generate
        val updatedModel = model.copy(model2Id = model2._id)
        result(model2s.collection.insert(model2))
        result(model1s.safeSave(updatedModel)) should beEqualTo(true)
        result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get must equalTo(updatedModel)
      }
    }

    ".update" >> {
      val model = model1s.generate
      result(model1s.update(model)) should beEqualTo(false)

      result(model1s.collection.insert(model))

      val updatedModel = model.copy(model2Id = BSONObjectID.generate)
      result(model1s.update(updatedModel)) should beEqualTo(true)
      result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get must equalTo(updatedModel)
    }

    ".safeUpdate" >> {
      val model = model1s.generate
      "fails with corrupted model" >> {
        result(model1s.collection.insert(model))
        result(model1s.safeUpdate(model)) should equalTo(false)
      }
      "success with valid model" >> {
        val model2 = model2s.generate
        val updatedModel = model.copy(model2Id = model2._id)
        result(model2s.collection.insert(model2))

        result(model1s.safeUpdate(updatedModel)) should equalTo(true)
        result(model1s.collection.find(BSONDocument("_id" -> model._id)).one[Model1]).get should equalTo(updatedModel)
      }
    }
  }
}
