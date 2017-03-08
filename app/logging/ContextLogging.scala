package logging

import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.AnyPlan
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ContextLogging extends LazyLogging {

  // as long as there's an implicit subscription context available, log will output the subscription name automatically
  def info[P <: AnyPlan : Subscription](message: String): Unit = {
    val context = implicitly[Subscription[P]]
    logger.info(s"{sub: ${context.name.get}} $message")
  }

  def error[P <: AnyPlan : Subscription](message: String): Unit = {
    val context = implicitly[Subscription[P]]
    logger.info(s"{sub: ${context.name.get}} $message")
  }

  implicit class FutureContextLoggable[T](future: Future[T]) {
    def withContextLogging[P <: AnyPlan : Subscription](message: String, extractor: T => Any = identity): Future[T] = {
      future.foreach(any => info(s"$message {${extractor(any)}}"))
      future
    }

  }

  implicit class ContextLoggable[T](t: T) {
    def withContextLogging[P <: AnyPlan : Subscription](message: String): T = {
      t match {
        case future: Future[_] => future.foreach(any => info(s"$message {$any}"))
        case any => info(s"$message {$any}")
      }
      t
    }

  }

}
