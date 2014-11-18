package lib.play2auth

import jp.t2v.lab.play2.auth._
import lib.util.BearerTokenGenerator
import models.{Session, Sessions}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.reflect.ClassTag


/**
 * Created by gaetansenn on 06/08/2014.
 */
object MongoIdContainer {
  lazy val sessions = Sessions(ReactiveMongoPlugin.db)
}

//Only support string for the moment
class MongoIdContainer[Id: ClassTag](sessions: Sessions = MongoIdContainer.sessions) extends IdContainer[Id] {

  def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    val token = BearerTokenGenerator.generateToken
    val user:BSONObjectID = userId match {
      case bson:BSONObjectID => bson
      case string:String => BSONObjectID(string)
      case _ => throw new RuntimeException("Id format not supported.")
    }
    sessions.removeByUser(user)
    val session = sessions.create(Session(token = token, userId = user))
    import scala.concurrent.duration._
    Await.result(session, 10.second).token
  }

  def remove(token: AuthenticityToken): Unit = {
    sessions.removeByToken(token)
  }

  def get(token: AuthenticityToken): Option[Id] = {
    val session = sessions.findByToken(token).map {
      case Some(session) => Some(session.userId.stringify.asInstanceOf[Id])
      case _ => None
    }
    import scala.concurrent.duration._
    Await.result(session, 10.second)
  }

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int): Unit = {
    val update = sessions.refreshToken(token, timeoutInSeconds)
    import scala.concurrent.duration._
    Await.result(update, 10.second)

  }
}
