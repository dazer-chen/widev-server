package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.{Administrator, Plugin, Plugins, Standard}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, Json, Reads}
import play.api.mvc.{BodyParsers, Controller}
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

/**
 * Created by trupin on 11/8/14.
 */
class PluginController(plugins: Plugins) extends Controller with AuthElement {
  self: AuthConfigImpl =>

  def createPlugin = AsyncStack(BodyParsers.parse.json, AuthorityKey -> Administrator) {
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
          plugins.create(Plugin(plugin.name, plugin.endPoint)).map {
            plugin => Ok(Json.toJson(plugin))
          }
        }
      )
  }

  def getPlugin(id: String) = AsyncStack( BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      plugins.find(BSONObjectID(id)).map {
        case Some(plugin) => Ok(Json.toJson(plugin))
        case None => NotFound(s"Couldn't find plugin for id: $id")
      }
  }

  def getPlugins = AsyncStack( BodyParsers.parse.json, AuthorityKey -> Standard) {
    implicit request =>
      plugins.list.map(list => Ok(Json.toJson(list)))
  }

}

object PluginController extends PluginController(Plugins(ReactiveMongoPlugin.db)) with AuthConfigImpl
