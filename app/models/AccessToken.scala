package models

import org.joda.time.DateTime
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.{ExecutionContext}

/**
 * Created by trupin on 7/26/14.
 */
case class AccessToken(
                        accessToken: String,
                        userId: BSONObjectID,
                        clientId: BSONObjectID,
                        expiresIn: Long,
                        refreshToken: Option[String] = None,
                        scope: Option[String] = None,
                        createdAt: DateTime = DateTime.now
                        )

object AccessToken {

  implicit val handler = Macros.handler[AccessToken]

  def convert(token: AccessToken): scalaoauth2.provider.AccessToken = scalaoauth2.provider.AccessToken(
    token = token.accessToken,
    refreshToken = token.refreshToken,
    scope = token.scope,
    expiresIn = Some(token.expiresIn),
    createdAt = token.createdAt.toDate
  )

  def findToken(userId: BSONObjectID, clientId: BSONObjectID)(collection: BSONCollection)(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("userId" -> userId, "clientId" -> clientId)).one[AccessToken]

  def deleteExistingAndCreate(accessToken: AccessToken)(collection: BSONCollection)(implicit ec: ExecutionContext) =
    collection.remove(BSONDocument("userId" -> accessToken.userId, "clientId" -> accessToken.clientId)).map {
      _ => collection.insert(AccessToken)
    }
}
