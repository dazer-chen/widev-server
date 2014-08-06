package models

import lib.mongo.{SuperCollection, CannotEnsureRelation, Collection}
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONObjectID, Macros}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by trupin on 7/26/14.
 */
case class AccessToken(
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
    import play.api.libs.concurrent.Execution.Implicits._
    val factory = Factory(db)
    Seq(
      factory.users,
      factory.clients
    )
  }

  def generate = AccessToken(
    accessToken = BSONObjectID.generate.stringify,
    userId = BSONObjectID.generate,
    clientId = BSONObjectID.generate
  )

  def find(userId: BSONObjectID, clientId: BSONObjectID)(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("userId" -> userId, "clientId" -> clientId)).one[AccessToken]

  def deleteExistingAndCreate(accessToken: AccessToken)(implicit ec: ExecutionContext) =
    collection.remove(BSONDocument("userId" -> accessToken.userId, "clientId" -> accessToken.clientId)).map {
      _ => collection.insert(accessToken)
    }

  def findRefreshToken(token: String)(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("refreshToken" -> token)).one[AccessToken]

  def findByToken(token: String)(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("accessToken" -> token)).one[AccessToken]

  def find(clientId: BSONObjectID, scope: Option[String])(implicit ec: ExecutionContext) =
    collection.find(BSONDocument("clientId" -> clientId) ++ (
      if (scope.nonEmpty) BSONDocument("scope" -> scope.get) else BSONDocument()
      )).one[AccessToken]

}
