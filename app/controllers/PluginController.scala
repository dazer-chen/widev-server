package controllers

import models.{Plugin, Plugins}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc.{Action, BodyParsers, Controller}
import play.modules.reactivemongo.ReactiveMongoPlugin

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current

/**
 * Created by trupin on 11/8/14.
 */
class PluginController(plugins: Plugins) extends Controller {
  def register = Action.async(BodyParsers.parse.json) {
    implicit request =>

      case class CreatePlugin(name: String, endPoint: String)

      implicit val createTeamReads: Reads[CreatePlugin] = (
        (JsPath \ "name").read[String] and
          (JsPath \ "endPoint").read[String]
        )(CreatePlugin.apply _)

      val plugin = request.body.validate[CreatePlugin]

      plugin.fold(
        errors => {
          Future(BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(errors))))
        },
        plugin => {
          plugins.update(Plugin(plugin.name, plugin.endPoint), upsert = true).map {
            plugin => Ok(Json.toJson(plugin))
          }
        }
      )
  }
}

object PluginController extends PluginController(Plugins(ReactiveMongoPlugin.db))
