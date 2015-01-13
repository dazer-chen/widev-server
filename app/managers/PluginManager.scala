package managers

import models.{Team, User, Plugins}
import play.api.http.{HeaderNames, ContentTypeOf, Writeable}
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WS
import play.api.libs.ws.WSRequestHolder
import play.api.mvc.Request
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by trupin on 11/8/14.
 */
class PluginManager(plugins: Plugins) {

  private[managers] def broadcastToAll(method: String, path: String, request: (WSRequestHolder => WSRequestHolder)) = plugins.list.flatMap {
    plugins =>
      Future.sequence(plugins.map {
        plugin =>
          request(WS.url(plugin.endPoint + '/' + plugin.name + path))
            .withRequestTimeout(10000)
            .withFollowRedirects(follow = true)
            .withHeaders(HeaderNames.CONTENT_TYPE -> "application/json")
            .execute(method)
      }).map {
        _ => {
          println("Request done.")
        }
      }
  }

  def createTeam(user: User, team: Team) = broadcastToAll("POST", "/teams", (request: WSRequestHolder) => {
    val body = Json.obj(
    "userId" -> JsString(user._id.stringify),
    "id" -> JsString(team._id.stringify),
    "name" -> JsString(team.name)
    )

    request.withBody(body)
  })

  def createUser(user: User) = broadcastToAll("POST", "/users", (request: WSRequestHolder) => {

    val body = Json.obj(
      "id" -> JsString(user._id.stringify),
      "email" -> JsString(user.email),
      "password" -> JsString(user.password),
      "firstName" -> JsString(user.firstName.get),
      "lastName" -> JsString(user.lastName.get)
    )

    request.withBody(body)
  })
}

object PluginManager extends PluginManager(new Plugins(ReactiveMongoPlugin.db))