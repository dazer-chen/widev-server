package controllers

import db.{DbAlreadyUpToDate, Migrations}
import jp.t2v.lab.play2.auth.AuthElement
import models.Administrator
import play.api.mvc.{BodyParsers, Action, Controller}
import play.modules.reactivemongo.MongoController

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 8/2/14.
 */
object MigrationController extends Controller with AuthElement with AuthConfigImpl {
  self: AuthConfigImpl =>

  def run = AsyncStack(AuthorityKey -> Administrator) {
    implicit request =>
      Migrations.run().map {
        migrations => Ok(migrations.toString())
      }.recover {
        case e: DbAlreadyUpToDate => Ok(e.getMessage)
      }
  }
}
