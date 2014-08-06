package lib

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Created by trupin on 8/3/14.
 */
trait Util {
  def result[R](f: Future[R])(implicit ec: ExecutionContext, timeout: Duration = 10.seconds): R = Await.result[R](f, timeout)
}
