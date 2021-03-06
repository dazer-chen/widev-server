package lib.util

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json._
import reactivemongo.bson.{BSONDateTime, BSONHandler}

/**
 * Created by trupin on 7/26/14.
 */
object Implicits {
  implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(d: BSONDateTime) = new DateTime(d.value)
    def write(d: DateTime) = BSONDateTime(d.getMillis)
  }

  implicit object JSONDateTimeWriter extends Writes[DateTime] {
    override def writes(o: DateTime): JsValue = Json.toJson(o.toString("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'"))
  }

  implicit object JSONDateTimeReader extends Reads[DateTime] {
    override def reads(json: JsValue): JsResult[DateTime] =
      JsSuccess(DateTime.parse(json.toString(), DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")))
  }
}