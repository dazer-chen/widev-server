package controllers

import play.api.mvc._
import play.modules.reactivemongo._
import reactivemongo.api.collections.default.BSONCollection

object Application extends Controller with MongoController {

  def collection: BSONCollection = db.collection("users")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}