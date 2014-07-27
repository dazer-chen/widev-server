package lib.oauth

import lib.util.Crypto
import models.{GrandType, Client, User}
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaoauth2.provider.{AccessToken, AuthInfo, DataHandler}

/**
 * Created by trupin on 7/26/14.
 */
case class OAuth2ProviderDataHandler(
                                      clients: BSONCollection,
                                      users: BSONCollection,
                                      accessTokens: BSONCollection
                                      ) extends DataHandler[User] {

  val timeout = 100.milliseconds

  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean =
    Await.result(Client.validate(BSONObjectID(clientId), clientSecret, GrandType(grantType))(clients), timeout)

  def findUser(username: String, password: String): Option[User] =
    Await.result(User.findUser(username, password)(users), timeout)

  def createAccessToken(authInfo: AuthInfo[User]): AccessToken = {
    val token = models.AccessToken(
      userId = authInfo.user._id,
      clientId = BSONObjectID(authInfo.clientId),
      accessToken = Crypto.generateToken(),
      refreshToken = Some(Crypto.generateToken()),
      scope = authInfo.scope,
      expiresIn = (60 * 60).toLong
    )
    Await.result(models.AccessToken.deleteExistingAndCreate(token)(accessTokens), timeout)
    models.AccessToken.convert(token)
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Option[AccessToken] =
    Await.result(models.AccessToken.findToken(userId = authInfo.user._id, clientId = BSONObjectID(authInfo.clientId))(accessTokens).map {
      case Some(token) => Some(models.AccessToken.convert(token))
      case _ => None
    }, timeout)

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): AccessToken =
    createAccessToken(authInfo)

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] =


  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] = ???

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] = ???

  def findAccessToken(token: String): Option[AccessToken] = ???

  def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[User]] = ???
}
