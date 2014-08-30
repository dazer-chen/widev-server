/**
 * Created by trupin on 8/30/14.
 */

import lib.mongo.DuplicateModel
import play.api._
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

object Global extends GlobalSettings {
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful {
      ex match {
        case e: DuplicateModel => NotAcceptable("Model already exists.")
        case _ => InternalServerError("An error occurred, we're working to repair this as soon as possible.")
      }
    }
  }
}
