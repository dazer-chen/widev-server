package models

import org.joda.time.DateTime
import reactivemongo.bson.{BSONObjectID, Macros}

/**
 * Created by trupin on 7/26/14.
 */
case class AccessToken(
                        accessToken: String,
                        userId: BSONObjectID,
                        clientId: BSONObjectID,
                        expiresIn: Int,
                        refreshToken: Option[String] = None,
                        scope: Option[String] = None,
                        createdAt: DateTime = DateTime.now
                        )

object AccessToken {
  import lib.util.Implicits.BSONDateTimeHandler

  implicit val handler = Macros.handler[AccessToken]
}
