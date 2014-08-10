package lib.play2auth

import java.awt.Container
import java.security.SecureRandom

import jp.t2v.lab.play2.auth._
import models.{Session, Sessions}
import play.api.Play.current
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext}
import scala.reflect.ClassTag
import reflect.ClassTag

import play.api.libs.concurrent.Execution.Implicits._


/**
 * Created by gaetansenn on 06/08/2014.
 */

object BearerTokenGenerator {

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken:String =
    generateToken(TOKEN_LENGTH)

  def generateToken(tokenLength: Int): String =
    if(tokenLength == 0) "" else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
      generateToken(tokenLength - 1)

}

//Only support string for the moment
class MongoIdContainer[Id: ClassTag] extends IdContainer[Id] {

  implicit def db = ReactiveMongoPlugin.db
  lazy val sessions = Sessions(db)

  def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    val token = BearerTokenGenerator.generateToken
    val user:BSONObjectID = userId match {
      case bson:BSONObjectID => bson
      case string:String => BSONObjectID(string)
      case long:Long => BSONObjectID(long.toString)
    }
    sessions.removeByUser(user)
    val session = sessions.create(Session(token = token, userId = user))
    import scala.concurrent.duration._
    Await.result(session, 10.second)
    token
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
    val update = sessions.refreshToken(token)
    import scala.concurrent.duration._
    Await.result(update, 10.second)

  }
}
