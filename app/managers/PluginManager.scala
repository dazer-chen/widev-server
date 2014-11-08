package managers

import models.Plugins
import play.api.libs.ws.WS
import play.api.mvc.Request
import play.modules.reactivemongo.ReactiveMongoPlugin

/**
 * Created by trupin on 11/8/14.
 */
class PluginManager(plugins: Plugins) {

  def broadcastToAll(request: Request) = plugins.list.map(_.foreach {
    plugin =>
      WS.url(plugin.endPoint + request.path)
        .withHeaders(request.headers.toSimpleMap.toSeq:_*)
        .withRequestTimeout(10000)
        .withQueryString(request.queryString.toSeq.map { case (k, v) => (k, v.mkString(",")) }:_*)
        .execute(request.method)
  })

}

object PluginManager extends PluginManager(new Plugins(ReactiveMongoPlugin.db))