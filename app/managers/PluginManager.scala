package managers

import models.Plugins
import play.api.libs.ws.WS
import play.api.mvc.Request
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by trupin on 11/8/14.
 */
class PluginManager(plugins: Plugins) {

  def broadcastToAll[T](request: Request[T]) = plugins.list.flatMap {
    plugins =>
      Future.sequence(plugins.map {
        plugin =>
          WS.url(plugin.endPoint + request.path)
            .withHeaders(request.headers.toSimpleMap.toSeq:_*)
            .withRequestTimeout(10000)
            .withQueryString(request.queryString.toSeq.map { case (k, v) => (k, v.mkString(",")) }:_*)
            .execute(request.method)
      }).mapTo[Unit]
  }
}

object PluginManager extends PluginManager(new Plugins(ReactiveMongoPlugin.db))