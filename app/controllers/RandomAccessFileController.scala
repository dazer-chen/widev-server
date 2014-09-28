package controllers

import akka.actor.ActorSystem
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller, WebSocket}
import redis.RedisClient
import utils.{FileHeader, RequestHandler, RequestHandlerException}

import scala.concurrent.Future

/**
 * Created by gaetansenn on 28/09/14.
 */

object RandomAccessFileController extends Controller {

  implicit lazy val system = ActorSystem("play-redis")

  lazy val conf = play.Play.application().configuration()

  lazy val clients = RedisClient.apply(host = conf.getString("redis.host"), port = conf.getString("redis.port").toInt)


  def write = WebSocket.using[Array[Byte]] { request =>

    val (out, channel) = Concurrent.broadcast[Array[Byte]]

    val in = Iteratee.foreach[Array[Byte]] {
      request => try {
        RequestHandler.read(request)
        channel push(request)
      } catch {
        case e : RequestHandlerException => {
          channel push(RequestHandler.createResponseHeader(Json.obj("error" -> e.message)))
        }
      }
    }
    (in,out)
  }

  def read = Action.async {

    clients.set[FileHeader]("toto", FileHeader(filename = "test", size = 12)).map {
      response => if (response) {
        clients.get[FileHeader]("toto").map {
          case Some(a) => println(a.filename)
          case None => NotFound("Not FOund toto")
        }
      } else {
        NotFound("Fail")
      }
    }


    Future(Ok(""))
  }
}
