package services

import com.gu.okhttp.RequestRunners.futureRunner
import com.typesafe.scalalogging.StrictLogging
import configuration.Config
import forms.ReportDeliveryProblem
import model.FulfilmentLookup
import okhttp3.{MediaType, RequestBody}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsError, JsSuccess, Json}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scalaz.{-\/, \/, \/-}

object FulfilmentLookupService extends StrictLogging {

  val httpClient = futureRunner

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

  def lookupSubscription(env: String, deliveryProblem: ReportDeliveryProblem): Future[String \/ FulfilmentLookup] = {
    val request = buildRequest(env, deliveryProblem)
    val futureResponse = httpClient(request)
    futureResponse.map { response =>
      val responseBody = response.body.string
      response.body.close
      if (response.isSuccessful) {
        logger.info(s"[${env}] Successfully performed lookup for ${deliveryProblem.subscriptionName}")
        val jsonBody = Json.parse(responseBody)
        jsonBody.validate[FulfilmentLookup] match {
          case validLookup: JsSuccess[FulfilmentLookup] =>
            \/-(validLookup.value)
          case error: JsError =>
            -\/(s"response was successful but body could not be parsed, we got: $responseBody")
        }
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
