package utils

import scala.concurrent.Future
import scala.util.Failure
import scala.concurrent.duration._
import dispatch._
import Defaults.timer
import com.typesafe.scalalogging.LazyLogging
import dispatch.retry
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Retry extends LazyLogging{
  def apply[A](count: Int, errMsg: String)(request: => Future[A]): Future[A] =
    retry.Backoff(max = count, delay = 2.seconds, base = 2) { () =>
      request.either
    } flatMap(_.fold(Future.failed(_), Future.successful(_))) andThen {case Failure(e) => logger.error(errMsg, e)}
}
