package db

import reactivemongo.api.DefaultDB

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by trupin on 8/2/14.
 */
trait Migration {
  def run(db: DefaultDB)(implicit ec: ExecutionContext): Future[Unit]
}
