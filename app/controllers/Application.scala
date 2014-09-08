package controllers

import fly.play.aws.auth.AwsCredentials
import fly.play.s3.{BucketFile, S3}
import jp.t2v.lab.play2.auth.AuthElement
import models._
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller with AuthElement with AuthConfigImpl {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def loginSuccess = Action {
    Ok(Json.obj("login" -> "success"))
  }

  def loginFail = Action {
    Ok(Json.obj("login" -> "fail"))
  }

  def AuthenticateTest = StackAction(AuthorityKey -> Visitor) { response =>
    Ok("ok")
  }

  def getFileTest = Action.async {
    import play.api.libs.concurrent.Execution.Implicits._

    val bucket = S3("widev-fs")(AwsCredentials.apply("AKIAJJFHVLQ6BJL4K3CA", "i/VNaA7GLzPcl48G0Vlrnq3YvIBbjtalzeSAyXAG"))

    bucket.get("id.jpg").map {
      case BucketFile(name, contentType, content, acl, headers) =>
        Ok(content)
    }

  }

}