/**
 * Created by trupin on 8/30/14.
 */

import lib.mongo.DuplicateModel
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import play.filters.gzip.GzipFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Global extends GlobalSettings {
  val loggingFilter = Filter { (nextFilter, requestHeader) =>
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      Logger.info(s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms " +
        s"and returned ${result.header.status}")
      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }

  override def onStart(app: Application): Unit = {
//    Migrations.runOnFirstApplicationStart()
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), loggingFilter, new GzipFilter())
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful {
      ex match {
        case e: DuplicateModel => NotAcceptable("Model already exists.")
        case _ => InternalServerError("An error occurred, we're working to repair this as soon as possible.")
      }
    }
  }
}
