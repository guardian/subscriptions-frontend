package services

import com.typesafe.scalalogging.StrictLogging
import configuration.Config
import forms.ReportDeliveryProblem
import okhttp3.{MediaType, Request, RequestBody, Response}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

object LookupSubscriptionFulfilment extends StrictLogging {

  def buildRequest(env: String, deliveryProblem: ReportDeliveryProblem): okhttp3.Request = {
    val apiDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val formattedDate = apiDateFormatter.print(deliveryProblem.issueDate)
    val json = Json.obj(
      "subscriptionName" -> deliveryProblem.subscriptionName.get,
      "sfContactId" -> deliveryProblem.sfContactId,
      "issueDate" -> formattedDate
    )
    val body = RequestBody.create(MediaType.parse("application/json"), json.toString())
    new okhttp3.Request.Builder()
      .addHeader("x-api-key", Config.fulfilmentLookupApiKey(env))
      .url(s"${Config.fulfilmentLookupApiUrl(env)}/fulfilment-lookup")
      .post(body)
      .build()
  }

  def apply(env: String, httpClient: Request => Future[Response], deliveryProblem: ReportDeliveryProblem)(implicit executionContext: ExecutionContext): Future[String \/ Unit] = {
    val request = buildRequest(env, deliveryProblem)
    val futureResponse = httpClient(request)
    futureResponse.map { response =>
      val responseBody = response.body.string
      response.body.close
      if (response.isSuccessful) {
        logger.info(s"[${env}] Successfully performed lookup for ${deliveryProblem.subscriptionName}")
        \/-(())
      } else {
        -\/(s"response was unsuccessful, we got a ${response.code}: ${responseBody}")
      }
    }.recoverWith {
      case ex: Exception => Future {
        -\/(s"future failed with: ${ex}")
      }
    }
  }


}
