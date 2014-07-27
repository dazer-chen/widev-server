package models

import lib.Collection
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.bson._

/**
 * Created by trupin on 7/26/14.
 */
case class AuthCode(
                     authorizationCode: String,
                     userId: BSONObjectID,
                     clientId: BSONObjectID,
                     expiresIn: Int,
                     redirectUri: Option[String] = None,
                     scope: Option[String] = None,
                     createdAt: DateTime = DateTime.now
                     )

object AuthCode {
  import lib.util.Implicits.BSONDateTimeHandler

  implicit val handler = Macros.handler[AuthCode]
}

case class AuthCodeCollection(db: DefaultDB) extends Collection(db) {
  val collectionName = "auth-codes"
}