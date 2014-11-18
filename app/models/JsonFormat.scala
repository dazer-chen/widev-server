package models

/**
 * Created by gaetansenn on 02/08/2014.
 */

import play.api.libs.json._
import play.api.http.Status
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

trait JsonException extends Throwable

case class WrongErrorCode() extends JsonException

object JsonError {
  implicit val JsonErrorWrites = new Writes[JsonError] {
    def writes(model: JsonError) = Json.obj("error" -> Json.obj(
      "id" -> model.id,
      "href" -> model.href,
      "status" -> model.status,
      "code" -> model.code,
      "title" -> model.title,
      "detail" -> model.detail)
    )
  }

  implicit val jsonErrorRead: Reads[JsonError] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "href").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "code").read[Int] and
      (JsPath \ "title").read[String] and
      (JsPath \ "detail").read[String] and
      (JsPath \ "links").readNullable[List[String]]
    )(JsonError.apply _)
}

case class JsonError(id: Long,
                     href: String,
                     status: String,
                     code: Int,
                     title: String,
                     detail: String,
                     links: Option[List[String]]) {

  // Define all error code supported by the JsonError Class
  val ErrorsCode = Array(Status.BAD_REQUEST, Status.UNAUTHORIZED)

  def expectedErrorCode(): Boolean = {
      ErrorsCode contains (code)
  }

  if (!expectedErrorCode()) throw WrongErrorCode()
}


object JsonFormat {

    def generateError(jsonError : JsonError): JsValue = {
      Json.toJson(jsonError)
    }

    def generateResult[T](model : Object)(implicit tjs: Writes[T], m: reflect.Manifest[T]): JsValue = {
      Json.obj(m.toString() -> Json.toJson(model.asInstanceOf[T]))
    }
}
