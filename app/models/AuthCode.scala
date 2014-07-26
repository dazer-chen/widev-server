package models

import lib.util.Implicits
import org.joda.time.DateTime
import reactivemongo.bson.{BSONObjectID, Macros}

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
