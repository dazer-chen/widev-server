package models

import lib.mongo.{SuperCollection, Collection}
import lib.util.BearerTokenGenerator
import lib.util.Implicits.BSONDateTimeHandler
import org.joda.time.DateTime
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson._
import reactivemongo.core.commands.{RawCommand, LastError, Command}
import controllers.AuthConfigImpl

import scala.concurrent.{Future, ExecutionContext}

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by gaetansenn on 06/08/2014.
 */

case class Session(_id: BSONObjectID = BSONObjectID.generate,
                   token: String,
                   userId: BSONObjectID,
                   createdAt: DateTime = DateTime.now)

object Session {
  import lib.util.Implicits.BSONDateTimeHandler
  implicit val handler = Macros.handler[Session]
}

case class Sessions(db: DefaultDB) extends Collection[Session] with AuthConfigImpl {

  val collection = db.collection[BSONCollection]("session")

  def relations: Seq[SuperCollection] = Seq.empty

  def generate: Session = Session(
    token = BearerTokenGenerator.generateToken,
    userId = BSONObjectID.generate
  )

//  def commandExpireData(db: DefaultDB): Future[BSONDocument] = {
//    val command = BSONDocument(
//      "ensureIndex" -> "session",
//      "$pipeline" -> BSONDocument(
//        "createAt" -> "1",
//        "expireAfterSeconds" -> 30
//      )
//    )
//    db.command(RawCommand(command))
//  }

//  collection.indexesManager.ensure(
//    Index(List("createdAt" -> IndexType.Ascending))
//  )


  def cleanExpiredTokens(timeout: Option[Int]): Future[LastError] = {
    collection.remove(BSONDocument(
      "createdAt" ->
        BSONDocument("$lt" -> BSONDateTimeHandler.write(
        timeout match {
          case Some(seconds) => DateTime.now.minusSeconds(seconds)
          case None => DateTime.now.minusSeconds(sessionTimeoutInSeconds)
        }
          ))
    ))
  }

  def findByToken(token: String): Future[Option[Session]] = {
    val query = BSONDocument("$query" -> BSONDocument("token" -> token))
    collection.find(query).one[Session]
  }

  def refreshToken(token: String): Future[Option[Session]] = {
    findByToken(token).flatMap {
      case Some(session) => {
        val new_session = session.copy(createdAt = DateTime.now)
        update(new_session).map {
          case true => Some(new_session)
          case false => None
        }
      }
      case _ => Future(None)
    }
  }

  def removeByToken(token: String): Future[LastError] =
    collection.remove(BSONDocument("token" -> token))

  def removeByUser(user: BSONObjectID): Future[LastError] =
  collection.remove(BSONDocument("userId" -> user))

}