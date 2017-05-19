package logging

import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.AnyPlan
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.TouchpointBackend

import scala.concurrent.Future
import scala.language.implicitConversions

case class Context(
  sub: Subscription[AnyPlan],
  environmentName: String
)

trait ContextLogging extends StrictLogging {

  implicit def makeContext(implicit sub: Subscription[AnyPlan], resolution: TouchpointBackend.Resolution) = Context(sub, resolution.backend.environmentName)
  implicit def makeContextFromSub(sub: Subscription[AnyPlan])(implicit resolution: TouchpointBackend.Resolution) = makeContext(sub, resolution)

  // as long as there's an implicit subscription context available, log will output the subscription name automatically
  def info(message: String)(implicit context: Context) = log(logger.info(_), message, context)

  def error(message: String)(implicit context: Context) = log(logger.error(_), message, context)

  def error(message: String, cause: Throwable)(implicit context: Context) = log(logger.error(_, cause), message, context)

  private def log(log: String => Unit, message: String, context: Context): Unit = {
    log(s"{${context.environmentName} sub: ${context.sub.name.get}} $message")
  }

  // you can even run an extractor on the value in a future
  implicit class FutureContextLoggable[T](future: Future[T]) {
    def withContextLogging(message: String, extractor: T => Any = identity)(implicit context: Context): Future[T] = {
      future.foreach(any => info(s"$message {${extractor(any)}}"))
      future
    }

  }

  // if you do context logging on a future it will wait for the success and show the value
  implicit class ContextLoggable[T](t: T) {
    def withContextLogging(message: String)(implicit context: Context): T = {
      t match {
        case future: Future[_] => future.foreach(any => info(s"$message {$any}"))
        case any => info(s"$message {$any}")
      }
      t
    }

  }

}
