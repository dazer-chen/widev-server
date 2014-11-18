package db.migrations

import db.Migration
import models.{Standard, Administrator, User, Users}
import play.api.Play
import reactivemongo.api.DefaultDB

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 11/8/14.
 */
object M2_AddDefaultUsers extends Migration {
  override def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit] = {
    val users = Users(db)
    val config = Play.current.configuration

    val standardUser = User(
      email = config.getString("users.standard.email").get,
      password = config.getString("users.standard.password").get,
      firstName = config.getString("users.standard.firstName"),
      lastName = config.getString("users.standard.lastName"),
      permission = Standard
    )

    val administratorUser = User(
      email = config.getString("users.administrator.email").get,
      password = config.getString("users.administrator.password").get,
      firstName = config.getString("users.administrator.firstName"),
      lastName = config.getString("users.administrator.lastName"),
      permission = Administrator
    )

    Future.sequence(Seq(
      users.create(standardUser),
      users.create(administratorUser)
    )).mapTo[Unit]
  }
}
