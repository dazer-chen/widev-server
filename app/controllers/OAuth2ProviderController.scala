package controllers

import lib.oauth.OAuth2ProviderDataHandler
import models.{AuthCodes, AccessTokens, Users, Clients}
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import scalaoauth2.provider.OAuth2Provider

import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by trupin on 7/26/14.
 */
object OAuth2ProviderController extends Controller with OAuth2Provider with MongoController {
  def accessToken = Action { implicit request =>
    issueAccessToken(new OAuth2ProviderDataHandler(
      clients = Clients(db),
      users = Users(db),
      accessTokens = AccessTokens(db),
      authCodes = AuthCodes(db)
    ))
  }
}
