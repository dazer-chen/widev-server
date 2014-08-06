package lib.mongo

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 7/27/14.
 */
sealed abstract class CollectionException(message: String) extends RuntimeException(message)

case class CannotEnsureRelation(collection: String) extends CollectionException(s"'$collection': Cannot ensure relation between collections.")
case class InvalidModel(message: String) extends CollectionException(message)

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
  def exists[T](field: String, value: T)(implicit ec: ExecutionContext, writer: BSONWriter[T, BSONValue]): Future[Boolean] =
    collection.find(BSONDocument(field -> value), BSONDocument(field -> true)).one[BSONDocument].map {
      case Some(_) => true
      case _ => false
    }

  def exists(field: String, value: BSONValue)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.find(BSONDocument(field -> value), BSONDocument(field -> true)).one[BSONDocument].map {
      case Some(_) => true
      case _ => false
    }

  def exists(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] = exists("_id", id)

  /**
   * Inserts a model.
   */
  def createDocument(document: BSONDocument)(implicit ec: ExecutionContext): Future[BSONDocument] =
    collection.insert(document).map { _ => document }

  /**
   * Tries to insert a model and create random sub models if necessary.
   *
   * @param document model to insert
   */
  def deepCreateDocument(document: BSONDocument)(parents: Set[String] = Set.empty)(implicit ec: ExecutionContext): Future[BSONDocument] =
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        if (parents.contains(cName))
          c.createDocument(generateDocument)
        else
          c.deepCreateDocument(generateDocument)(parents + cName)
    }).map {  _ => document }

  /**
   * Deletes a model from its id.
   *
   * @param id model's id
   */
  def delete(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] =
    collection.remove(BSONDocument("_id" -> id)).map {
      case res if res.n > 0 => true
      case _ => false
    }

  /**
   * Recursively deletes a model and all its _relations.
   */
  def deepDeleteFromDocument(document: BSONDocument)(implicit ec: ExecutionContext): Future[Boolean] =
    deepDeleteInternalFromDocument(document)(Set.empty)

  def deepDelete(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] =
    deepDeleteInternal(id)(Set.empty)

  private def deepDeleteInternalFromDocument(document: BSONDocument)(parents: Set[String] = Set.empty)(implicit ec: ExecutionContext): Future[Boolean] =
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        if (parents.contains(cName))
          c.delete(value)
        else
          c.deepDeleteInternal(value)(parents + cName)
    }).map(_.contains(true))

  private def deepDeleteInternal(id: BSONObjectID)(parents: Set[String])(implicit ec: ExecutionContext): Future[Boolean] =
    findDocument(id).flatMap {
      case Some(res) => deepDeleteInternalFromDocument(res)(parents)
      case _ => Future(true)
    }

  /**
   * Finds a model from its id.
   *
   * @param id model's id
   * @return true if at least one element has been deleted
   */
  def findDocument(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[BSONDocument]] = collection.find(BSONDocument("_id" -> id)).one[BSONDocument]

  protected def collectionNameToIdName(name: String) = {
    val n = name.split("-").map(s => s.substring(0, 1).capitalize + s.substring(1)).mkString.replace("s$", "")
    (if (n.last == 's') n.dropRight(1) else n) + "Id"
  }

  protected def getIdFromDocument(idName: String, document: BSONDocument): BSONObjectID =
    document.getAs[BSONObjectID](idName) match {
      case Some(value) => value
      case _ => throw InvalidModel(s"Couldn't find the field: '$idName'. in the document.")
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
  def deepCreate(model: M)(implicit ec: ExecutionContext): Future[M] = {
    val document = writer.write(model)
    deepCreateDocument(document)(Set(collection.name)).map(reader.read)
  }

  /**
   * Inserts a model only if every _relations are ensured.
   *
   * @param model model to insert
   */
  def safeCreate(model: M)(implicit ec: ExecutionContext): Future[Boolean] = {
    val document = writer.write(model)
    Future.sequence(_relations.map {
      case c =>
        val cName = c.collection.name
        val name = collectionNameToIdName(cName)
        val value = getIdFromDocument(name, document)
        c.exists("_id", value)
    }).map {
      case res if !res.contains(false) =>
        create(model)
        true
      case _ => false
    }
  }

  /**
   * Finds a model from its id.
   *
   * @param id model's id
   */
  def find(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[M]] =
    findDocument(id).map {
      case Some(res) => Some(reader.read(res))
      case _ => None
    }

  /**
   * Inserts a model
   *
   * @param model to insert
   */
  def create(model: M)(implicit ec: ExecutionContext): Future[M] =
    createDocument(writer.write(model)).map(reader.read)

  /**
   * Inserts a model if it doesn't exist, otherwise, updates it.
   *
   * @param model model to save
   */
  def save(model: M)(implicit ec: ExecutionContext): Future[M] =
    collection.save(model).map { _ => model }

  /**
   * Updates a model.
   *
   * @param model to update
   */
  def update(model: M)(implicit ec: ExecutionContext): Future[M] = {
    val document = writer.write(model)
    val id = getIdFromDocument("_id", document)
    collection.update(BSONDocument("_id" -> id), model).map { _ => model}
  }
}