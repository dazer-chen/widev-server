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
  implicit val handler = Macros.handler[AuthCode]
}

case class AuthCodeCollection[S](db: DefaultDB) extends Collection[S](db) {
  val collectionName = "auth-codes"


}