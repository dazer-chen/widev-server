package models

import lib.mongo.Collection
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

  def cleanExpiredTokens = {

  }
}

case class Sessions(db: DefaultDB) extends Collection with AuthConfigImpl {

  val collection = db.collection[BSONCollection]("session")

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

//  createAt > DateTime.now - x second


  def cleanExpiredTokens(): Future[LastError] = {
    collection.remove(BSONDocument(
      "createdAt" ->
        BSONDocument("$lt" -> BSONDateTimeHandler.write(DateTime.now.minusSeconds(sessionTimeoutInSeconds)))
    ))
  }

  def create(session: Session): Future[Session] = {
    collection.insert[Session](session).map { _ => session }
  }

  def findByToken(token: String): Future[Option[Session]] = {
    val query = BSONDocument("$query" -> BSONDocument("token" -> token))
    collection.find(query).one[Session]
  }

  def updateCreateAt(id: BSONObjectID): Future[LastError] = {
    def modifier = BSONDocument(
      "$set" -> BSONDocument(
        "createAt" -> BSONDateTime(new DateTime().getMillis)
      )
    )

    collection.update(BSONDocument("_id" -> id), modifier)
  }

  def refreshToken(token: String): Future[Option[Session]] = {
    findByToken(token).flatMap {
      case Some(session) => {
        updateCreateAt(session._id).map { _ => Some(session) }
      }
      case _ => Future(None)
    }
  }

  def removeByToken(token: String): Future[LastError] =
    collection.remove(BSONDocument("token" -> token))

  def removeByUser(user: BSONObjectID): Future[LastError] =
  collection.remove(BSONDocument("userId" -> user))

}