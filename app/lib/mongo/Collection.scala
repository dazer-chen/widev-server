package lib.mongo

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 7/27/14.
 */
trait SuperCollection {

  val collection: BSONCollection

  def relations: Seq[SuperCollection]
  protected lazy val _relations = relations.toSet

  /**
   * Generates a model with random values.
   */
  def generateDocument: BSONDocument

  /**
   * Checks if a model exists.
   */
  def exists(field: String, value: BSONValue): Future[Boolean] =
    collection.find(BSONDocument(field -> value), BSONDocument(field -> true)).one[BSONDocument].map {
      case Some(_) => true
      case _ => false
    }

  def exists(id: BSONObjectID): Future[Boolean] = exists("_id", id)

  /**
   * Inserts a model.
   */
  private[mongo] def createDocument(document: BSONDocument): Future[BSONDocument] =
    collection.insert(document).map { _ =>
        document
    } recover {
      case e: DatabaseException if e.code.getOrElse(0) == 11000 =>
        throw new DuplicateModel(e.getMessage())
    }

  /**
   * Tries to insert a model and create random sub models if necessary.
   *
   * @param document model to insert
   */
  private[mongo] def deepCreateDocument(document: BSONDocument)(parents: Set[String] = Set.empty): Future[BSONDocument] = {
    if (parents.contains(collection.name))
      createDocument(document)
    else
      Future.sequence(_relations.map {
        case c =>
          val cName = c.collection.name
          val name = collectionNameToIdName(cName)
          val value = getIdFromDocument(name, document)
          val newDoc = lib.util.Mongo.overrideFieldWithBSON(c.generateDocument, ("_id", value))
          c.deepCreateDocument(newDoc)(parents + cName)
      }).map { _ => document}
  }

  /**
   * Deletes a model from its id.
   *
   * @param id model's id
   */
  def delete(id: BSONObjectID): Future[Boolean] =
    collection.remove(BSONDocument("_id" -> id)).map {
      case res if res.n > 0 => true
      case _ => false
    }

  /**
   * Recursively deletes a model and all its _relations.
   */
  private[mongo] def deepDeleteFromDocument(document: BSONDocument): Future[Boolean] =
    deepDeleteInternalFromDocument(document)(Set.empty)

  def deepDelete(id: BSONObjectID): Future[Boolean] =
    if (_relations.size > 0)
      deepDeleteInternal(id)(Set.empty)
    else
      delete(id)

  private[mongo] def deepDeleteInternalFromDocument(document: BSONDocument)(parents: Set[String] = Set.empty): Future[Boolean] = {
    if (!parents.contains(collection.name)) {
      Future.sequence(_relations.map {
        case c =>
          val cName = c.collection.name
          val name = collectionNameToIdName(cName)
          val value = getIdFromDocument(name, document)
          c.deepDeleteInternal(value)(parents + cName)
      }).flatMap {
        results =>
          delete(getIdFromDocument("_id", document)).map(_ && !results.contains(false))
      }
    }
    else
      delete(getIdFromDocument("_id", document))
  }

  private[mongo] def deepDeleteInternal(id: BSONObjectID)(parents: Set[String]): Future[Boolean] =
    findDocument(id).flatMap {
      case Some(res) => deepDeleteInternalFromDocument(res)(parents)
      case _ => Future(false)
    }

  /**
   * Finds a model from its id.
   *
   * @param id model's id
   * @return true if at least one element has been deleted
   */
  private[mongo] def findDocument(id: BSONObjectID): Future[Option[BSONDocument]] = collection.find(BSONDocument("_id" -> id)).one[BSONDocument]

  private[mongo] def collectionNameToIdName(name: String) = {
    val n = name.split("-").map(s => s.substring(0, 1).capitalize + s.substring(1)).mkString.replace("s$", "")
    val n1 = n.substring(0, 1).toLowerCase + n.substring(1)
    (if (n1.last == 's') n1.dropRight(1) else n1) + "Id"
  }

  private[mongo] def getIdFromDocument(idName: String, document: BSONDocument): BSONObjectID =
    document.getAs[BSONObjectID](idName) match {
      case Some(value) => value
      case _ => throw InvalidModel(s"Couldn't find the field: '$idName'. in the document: '${BSONDocument.pretty(document)}")
    }
}

abstract class Collection[M](implicit reader: BSONDocumentReader[M], writer: BSONDocumentWriter[M]) extends SuperCollection {

  def generate: M

  def generateDocument: BSONDocument = writer.write(generate)

  /**
   * Tries to insert a model and create random sub models if necessary.
   *
   * @param model model to insert
   */
  def deepCreate(model: M): Future[M] = {
    val document = writer.write(model)
    deepCreateDocument(document)(Set.empty).map(reader.read)
  }

  /**
   * Inserts a model only if every _relations are ensured.
   *
   * @param model model to insert
   */
  def safeCreate(model: M): Future[Boolean] = {
    val document = writer.write(model)
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        c.exists("_id", value)
    }).flatMap {
      case res if !res.contains(false) =>
        create(model).map { _ => true }
      case _ => Future(false)
    }
  }

  /**
   * Finds a model from its id.
   *
   * @param id model's id
   */
  def find(id: BSONObjectID): Future[Option[M]] =
    findDocument(id).map {
      case Some(res) => Some(reader.read(res))
      case _ => None
    }

  /**
   * Inserts a model
   *
   * @param model to insert
   */
  def create(model: M): Future[M] =
    createDocument(writer.write(model)).map(reader.read)

  /**
   * Inserts a model if it doesn't exist, otherwise, updates it.
   *
   * @param model model to save
   */
  def save(model: M): Future[M] =
    collection.save(model).map { _ => model }

  def safeSave(model: M): Future[Boolean] = {
    val document = writer.write(model)
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        c.exists("_id", value)
    }).flatMap {
      case res if !res.contains(false) =>
        save(model).map { _ => true }
      case _ => Future(false)
    }
  }


  /**
   * Updates a model.
   *
   * @param model to update
   */
  def update(model: M, upsert: Boolean = false): Future[Boolean] = {
    val document = writer.write(model)
    val id = getIdFromDocument("_id", document)
    collection.update(BSONDocument("_id" -> id), model, upsert = upsert).map {
      case res if res.n > 0 => true
      case _ => false
    }
  }

  def safeUpdate(model: M): Future[Boolean] = {
    val document = writer.write(model)
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        c.exists("_id", value)
    }).map {
      case res if !res.contains(false) =>
        update(model)
        true
      case _ => false
    }
  }
}