package controllers

import lib.oauth.OAuth2ProviderDataHandler
import play.api.mvc._
import scalaoauth2.provider.OAuth2Provider

/**
 * Created by trupin on 7/26/14.
 */
object OAuth2ProviderController extends Controller with OAuth2Provider {
  def accessToken = Action { implicit request =>
    issueAccessToken(new OAuth2ProviderDataHandler())
  }
}
