package services

import com.gu.membership.util.Timing
import com.gu.membership.zuora.Zuora._
import com.gu.membership.zuora.ZuoraDeserializer._
import com.gu.membership.zuora.ZuoraReaders._
import com.gu.membership.zuora.{Login, ZuoraAction, ZuoraApiConfig, ZuoraServiceError}
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import monitoring.TouchpointBackendMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

//todo move to mem-common?
class ZuoraService(val apiConfig: ZuoraApiConfig) {

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = apiConfig.envName

    val service = "Zuora"

    def recordError {
      put("error-count", 1)
    }
  }

  val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(request(Login(apiConfig)))


  def start() {
    authTask.start()
  }

  implicit def authentication: Authentication = authTask.get()

  def request[T <: ZuoraResult](action: ZuoraAction[T])(implicit reader: ZuoraReader[T]): Future[T] = {
    val url = if (action.authRequired) authentication.url else apiConfig.url

    if (action.authRequired && authentication.url.length == 0) {
      metrics.putAuthenticationError
      throw ZuoraServiceError(s"Can't build authenticated request for ${action.getClass.getSimpleName}, no Zuora authentication")
    }

    Timing.record(metrics, action.getClass.getSimpleName) {
      WS.url(url.toString).post(action.xml)
    }.map { result =>
      metrics.putResponseCode(result.status, "POST")

      reader.read(result.body) match {
        case Left(error) =>
          if (error.fatal) {
            metrics.recordError
            Logger.error(s"Zuora action error ${action.getClass.getSimpleName} with status ${result.status} and body ${action.sanitized}")
            Logger.error(result.body)
          }

          throw error

        case Right(obj) => obj
      }
    }
  }

}
