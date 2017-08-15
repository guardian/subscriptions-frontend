package services

import com.gu.okhttp.RequestRunners.futureRunner
import com.typesafe.scalalogging.StrictLogging
import configuration.Config
import forms.TrackDeliveryRequest
import model.FulfilmentLookup
import okhttp3.{MediaType, RequestBody}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsError, JsSuccess, Json}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scalaz.{-\/, \/, \/-}

object FulfilmentLookupService extends StrictLogging {

  val httpClient = futureRunner

  def buildRequest(environment: String, trackDelivery: TrackDeliveryRequest): okhttp3.Request = {
    val apiDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val formattedDate = apiDateFormatter.print(trackDelivery.issueDate)
    val body = RequestBody.create(MediaType.parse("application/json"), Json.obj("subscriptionName" -> trackDelivery.subscriptionName.get, "issueDate" -> formattedDate).toString())
    new okhttp3.Request.Builder()
      .addHeader("x-api-key", Config.fulfilmentLookupApiKey)
      .url(s"${Config.fulfilmentLookupApiUrl}/fulfilment-lookup")
      .post(body)
      .build()
  }

  def lookupSubscription(environment: String, trackDelivery: TrackDeliveryRequest): Future[String \/ FulfilmentLookup] = {
    val request = buildRequest(environment, trackDelivery)
    val futureResponse = httpClient(request)
    futureResponse.map { response =>
      val responseBody = response.body.string
      response.body.close
      if (response.isSuccessful) {
        logger.info(s"Successfully performed lookup for ${trackDelivery.subscriptionName}")
        val jsonBody = Json.parse(responseBody)
        jsonBody.validate[FulfilmentLookup] match {
          case validLookup: JsSuccess[FulfilmentLookup] =>
            \/-(validLookup.value)
          case error: JsError =>
            -\/(s"Response was successful but body could not be parsed, we got: $responseBody")
        }
      } else {
        -\/(s"Failed to perform delivery tracking for ${trackDelivery.subscriptionName}, we got: ${responseBody}")
      }
    }.recoverWith {
      case ex: Exception => Future {
        -\/(s"Failed to perform deliver tracking for ${trackDelivery.subscriptionName}, due to failed future: ${ex}")
      }
    }
  }


}
