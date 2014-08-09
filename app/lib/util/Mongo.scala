package lib.util

import reactivemongo.bson.{BSONWriter, BSONValue, BSONDocument}

/**
 * Created by trupin on 8/7/14.
 */
object Mongo {
  def overrideField[T](document: BSONDocument, field: (String, T))(implicit writer: BSONWriter[T, BSONValue]) =
    overrideFieldWithBSON(document, (field._1, writer.write(field._2)))

  def overrideFieldWithBSON(document: BSONDocument, field: (String, BSONValue)) =
    BSONDocument(document.elements.toTraversable.map {
      case (k, v) if k == field._1 => (k, field._2)
      case (k, v) => (k, v)
    })

}
