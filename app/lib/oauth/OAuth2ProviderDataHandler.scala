package lib.oauth

import lib.util.Crypto
import models._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._
import scalaoauth2.provider.{AccessToken, AuthInfo, DataHandler}

/**
 * Created by trupin on 7/26/14.
 */
case class OAuth2ProviderDataHandler(
                                      clients: Clients,
                                      users: Users,
                                      accessTokens: AccessTokens,
                                      authCodes: AuthCodes
                                      )
                                    (implicit ec: ExecutionContext) extends DataHandler[User] {

  val timeout = 100.milliseconds

  def validateClient(clientId: String, clientSecret: String, grantType: String): Boolean =
    Await.result(clients.validate(BSONObjectID(clientId), clientSecret, GrandType(grantType)), timeout)

  def findUser(username: String, password: String): Option[User] =
    Await.result(users.find(username, password), timeout)

  def createAccessToken(authInfo: AuthInfo[User]): AccessToken = {
    val token = models.AccessToken(
      userId = authInfo.user._id,
      clientId = BSONObjectID(authInfo.clientId),
      accessToken = Crypto.generateToken(),
      refreshToken = Some(Crypto.generateToken()),
      scope = authInfo.scope
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

  def findAuthInfoByCode(code: String): Option[AuthInfo[User]] =
    Await.result(authCodes.findByCode(code).flatMap {
      case Some(authCode) => users.findById(authCode.clientId).map {
        case Some(user) =>
          Some(AuthInfo(
            user = user,
            clientId = authCode.clientId.stringify,
            scope = authCode.scope,
            redirectUri = authCode.redirectUri
          ))
        case _ => None
      }
      case _ => Future(None)
    }, timeout)

  def findAuthInfoByRefreshToken(refreshToken: String): Option[AuthInfo[User]] =
    Await.result(accessTokens.findRefreshToken(refreshToken).flatMap {
      case Some(accessToken) =>
        users.findById(accessToken.userId).map {
          case Some(user) => Some(AuthInfo(
            user = user,
            clientId = accessToken.clientId.stringify,
            scope = accessToken.scope,
            redirectUri = None
          ))
          case _ => None
        }
      case _ => Future(None)
    }, timeout)

  def findClientUser(clientId: String, clientSecret: String, scope: Option[String]): Option[User] =
    Await.result(accessTokens.find(BSONObjectID(clientId), scope).flatMap {
      case Some(a) => users.findById(a.userId).flatMap {
        case Some(u) => clients.find(BSONObjectID(clientId), clientSecret, scope).map {
          case Some(_) => Some(u)
          case _ => None
        }
        case _ => Future(None)
      }
      case _ => Future(None)
    }, timeout)

  def findAccessToken(token: String): Option[AccessToken] =
    Await.result(accessTokens.findByToken(token).map {
      case Some(accessToken) => Some(models.AccessToken.convert(accessToken))
      case _ => None
    }, timeout)

  def findAuthInfoByAccessToken(accessToken: AccessToken): Option[AuthInfo[User]] =
    Await.result(accessTokens.findByToken(accessToken.token).flatMap {
      case Some(a) => users.findById(a.userId).map {
        case Some(user) => Some(AuthInfo(
          user = user,
          clientId = a.clientId.stringify,
          scope = a.scope,
          redirectUri = None
        ))
        case _ => None
      }
      case _ => Future(None)
    }, timeout)
}
