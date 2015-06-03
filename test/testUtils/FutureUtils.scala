package testUtils

import scala.concurrent._
import scala.concurrent.duration._

object FutureUtils {
  def await[T](f: Future[T]): T = Await.result(f, 1 second)
}
