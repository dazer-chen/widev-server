package services

import models._
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Future}
import play.api.Play.current

/**
 * Created by gaetansenn on 17/08/2014.
 */

class UserService(users: Users) {
  def find(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Option[User]] = users.find(id)
}

object UserService extends UserService(new Users(ReactiveMongoPlugin.db))
