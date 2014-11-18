package models

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.http.Status
import play.api.libs.json._

/**
 * Created by gaetansenn on 03/08/2014.
 */

@RunWith(classOf[JUnitRunner])
class JsonFormatSpec extends Specification {

  "JsonFormatSpec" should {

    "case class JsonError should raise an exception if the ErrorsCode not exist" in {
      lazy val jsonError = JsonError(1, "JsonErrorClass", "JsonErrorTest", Status.NOT_ACCEPTABLE, "JsonError", "", None)
      jsonError must throwA[WrongErrorCode]
    }
    "case class JsonError should not raise an exception if the ErrorsCode is correct" in {
      lazy val jsonError = JsonError(1, "JsonErrorClass", "JsonErrorTest", Status.BAD_REQUEST, "JsonError", "", None)
      jsonError must not (throwA[WrongErrorCode])
    }

    "generateError should return a JsValue with a parent error node" in {
      lazy val jsValue = JsonFormat.generateError(JsonError(1, "JsonErrorClass", "JsonErrorTest", Status.BAD_REQUEST, "JsonError", "", None))
      val errorReads: Reads[JsonError] = (JsPath \ "error").read[JsonError]
      jsValue.as[JsonError](errorReads) should beEqualTo(JsonError(1, "JsonErrorClass", "JsonErrorTest", Status.BAD_REQUEST, "JsonError", "", None))

    }

  }
}
