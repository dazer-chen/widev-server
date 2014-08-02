package controllers

import db.{DbAlreadyUpToDate, Migrations}
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 8/2/14.
 */
object Migration extends Controller with MongoController {

  def run = Action.async {
    Migrations.run(db).map {
      migrations => Ok(migrations.toString())
    }.recover {
      case e: DbAlreadyUpToDate => Ok(e.getMessage)
    }
  }
}
