package lib.mongo

/**
 * Created by trupin on 8/30/14.
 */
trait CollectionException extends Exception {
  val message: String

  override def getMessage: String = s"CollectionError[$message]"
}

class DuplicateModel(m: String) extends CollectionException {
  val message = s"DuplicateModel['$m']"
}

case class CannotEnsureRelation(collection: String) extends CollectionException {
  val message = s"CannotEnsureRelation['$collection: Cannot ensure relation between collections.']"
}

case class InvalidModel(m: String) extends CollectionException {
  val message = s"InvalidModel['$m']"
}
