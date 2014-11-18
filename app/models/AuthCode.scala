package models

import lib.mongo.{SuperCollection, Collection}
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._

import scala.concurrent.Future
import lib.util.Implicits.BSONDateTimeHandler
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by trupin on 7/26/14.
 */
case class AuthCode(
                     _id: BSONObjectID = BSONObjectID.generate,
                     authorizationCode: String,
                     userId: BSONObjectID,
                     clientId: BSONObjectID,
                     redirectUri: Option[String] = None,
                     scope: Option[String] = None,
                     createdAt: DateTime = DateTime.now,
                     expiresAt: DateTime = DateTime.now.plusHours(1)
                     )

object AuthCode {
  implicit val handler = Macros.handler[AuthCode]
}

case class AuthCodes(db: DefaultDB) extends Collection[AuthCode] {
  val collection = db.collection[BSONCollection]("auth-codes")

  def relations: Seq[SuperCollection] = {
    import play.api.libs.concurrent.Execution.Implicits._
    val factory = Factory(db)
    Seq(
      factory.users,
      factory.clients
    )
  }

  def generate = AuthCode(
    authorizationCode = BSONObjectID.generate.stringify,
    userId = BSONObjectID.generate,
    clientId = BSONObjectID.generate,
    redirectUri = Some(BSONObjectID.generate.stringify),
    scope = Some(BSONObjectID.generate.stringify)
  )

  def findValidByCode(code: String): Future[Option[AuthCode]] =
    collection.find(BSONDocument(
      "authorizationCode" -> code,
      "expiresAt" -> BSONDocument("$gte" -> DateTime.now)
    )).one[AuthCode]
}