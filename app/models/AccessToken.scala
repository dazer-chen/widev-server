package models

import lib.mongo.Collection
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by trupin on 7/26/14.
 */
case class AccessToken(
                        _id: BSONObjectID = BSONObjectID.generate,
                        accessToken: String,
                        userId: BSONObjectID,
                        clientId: BSONObjectID,
                        refreshToken: Option[String] = None,
                        scope: Option[String] = None,
                        createdAt: DateTime = DateTime.now,
                        expiresAt: DateTime = DateTime.now.plusHours(1)
                        )

object AccessToken {
  import lib.util.Implicits.BSONDateTimeHandler

  implicit val handler = Macros.handler[AccessToken]

  def convert(token: AccessToken): scalaoauth2.provider.AccessToken = scalaoauth2.provider.AccessToken(
    token = token.accessToken,
    refreshToken = token.refreshToken,
    scope = token.scope,
    expiresIn = Some((token.expiresAt.getMillis - token.createdAt.getMillis) / 1000),
    createdAt = token.createdAt.toDate
  )
}

case class AccessTokens(db: DefaultDB) extends Collection[AccessToken] {
  val collection = db.collection[BSONCollection]("access-tokens")

  def relations = {
    val factory = Factory(db)
    Seq(
      factory.users,
      factory.clients
    )
  }

  def generate = AccessToken(
    accessToken = BSONObjectID.generate.stringify,
    userId = BSONObjectID.generate,
    clientId = BSONObjectID.generate,
    refreshToken = Some(BSONObjectID.generate.stringify),
    scope = Some(BSONObjectID.generate.stringify)
  )

  def find(userId: BSONObjectID, clientId: BSONObjectID) =
    collection.find(BSONDocument("userId" -> userId, "clientId" -> clientId)).one[AccessToken]

  def deleteExistingAndCreate(accessToken: AccessToken) =
    collection.remove(BSONDocument("userId" -> accessToken.userId, "clientId" -> accessToken.clientId)).map {
      _ => collection.insert(accessToken)
    }

  def findByRefreshToken(token: String) =
    collection.find(BSONDocument("refreshToken" -> token)).one[AccessToken]

  def findByAccessToken(token: String) =
    collection.find(BSONDocument("accessToken" -> token)).one[AccessToken]

  def find(clientId: BSONObjectID, scope: Option[String]) =
    collection.find(BSONDocument("clientId" -> clientId) ++ (
      if (scope.nonEmpty) BSONDocument("scope" -> scope.get) else BSONDocument("scope" -> BSONDocument( "$exists" -> false ))
    )).one[AccessToken]

}
