package lib.oauth

import lib.util.Crypto
import models._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import scalaoauth2.provider.{AccessToken, AuthInfo, DataHandler}

/**
 * Created by trupin on 7/26/14.
 */
case class OAuth2ProviderDataHandler(
                                      clients: Clients,
                                      users: Users,
                                      accessTokens: AccessTokens
                                      )
                                    (implicit ec: ExecutionContext) extends DataHandler[User] {

  val timeout = 100.milliseconds

  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean =
    Await.result(clients.validate(BSONObjectID(clientId), clientSecret, GrandType(grantType)), timeout)

  def findUser(username: String, password: String): Option[User] =
    Await.result(users.findUser(username, password), timeout)

  def createAccessToken(authInfo: AuthInfo[User]): AccessToken = {
    val token = models.AccessToken(
      userId = authInfo.user._id,
      clientId = BSONObjectID(authInfo.clientId),
      accessToken = Crypto.generateToken(),
      refreshToken = Some(Crypto.generateToken()),
      scope = authInfo.scope,
      expiresIn = (60 * 60).toLong
    )
    Await.result(accessTokens.deleteExistingAndCreate(token), timeout)
    models.AccessToken.convert(token)
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[AccessToken] =
    Await.result(accessTokens.findToken(userId = authInfo.user._id, clientId = BSONObjectID(authInfo.clientId)).map {
      case Some(token) => Some(models.AccessToken.convert(token))
      case _ => None
    }, timeout)

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): AccessToken =
    createAccessToken(authInfo)

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] = ???


  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = ???

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = ???

  def findAccessToken(token: String): Option[AccessToken] = ???

  def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[User]] = ???
}
