package models

import reactivemongo.api.DefaultDB

import scala.concurrent.ExecutionContext

/**
 * Created by trupin on 8/2/14.
 */
case class Factory(db: DefaultDB)(implicit ec: ExecutionContext) {
  lazy val accessTokens = AccessTokens(db)
  lazy val authCodes = AuthCodes(db)
  lazy val clients = Clients(db)
  lazy val users = Users(db)
	lazy val session = Sessions(db)
	lazy val buckets = Buckets(db)
  lazy val teams = Teams(db)
}
