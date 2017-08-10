package services

import com.gu.okhttp.RequestRunners.futureRunner
import com.typesafe.scalalogging.StrictLogging
import configuration.Config
import forms.TrackDelivery
import model.FulfilmentLookup
import okhttp3.{MediaType, RequestBody}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsError, JsSuccess, Json}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scalaz.{-\/, \/, \/-}

object FulfilmentLookupService extends StrictLogging {

  val httpClient = futureRunner

  def buildRequest(environment: String, trackDelivery: TrackDelivery): okhttp3.Request = {
    val apiDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val formattedDate = apiDateFormatter.print(trackDelivery.issueDate)
    val body = RequestBody.create(MediaType.parse("application/json"), Json.obj("subscriptionName" -> trackDelivery.subscriptionName.get, "issueDate" -> formattedDate).toString())
    new okhttp3.Request.Builder()
      .addHeader("x-api-key", Config.fulfilmentLookupApiKey)
      .url(s"${Config.fulfilmentLookupApiUrl}/fulfilment-lookup")
      .post(body)
      .build()
  }

  def lookupSubscription(environment: String, trackDelivery: TrackDelivery): Future[String \/ FulfilmentLookup] = {
    val request = buildRequest(environment, trackDelivery)
    val futureResponse = httpClient(request)
    futureResponse.map { response =>
      if (response.isSuccessful) {
        logger.info(s"Successfully performed lookup for ${trackDelivery.subscriptionName}")
        val jsonBody = Json.parse(response.body.string)
        jsonBody.validate[FulfilmentLookup] match {
          case validLookup: JsSuccess[FulfilmentLookup] =>
            \/-(validLookup.value)
          case error: JsError =>
            logger.error("Failed to parse response as FulfilmentLookup")
            -\/(s"Response was successful but could not be parsed")
        }
      } else {
        val errorBody = response.body.string()
        response.body.close()
        val message = s"Failed to raise non-delivery case for subscription ${trackDelivery.subscriptionName} due to error: ${errorBody}"
        logger.error(message)
        -\/(message)
      }
    }.recoverWith {
      case ex: Exception => Future {
        val message = s"Future failed due to ${ex}"
        logger.error(message)
        -\/(message)
      }
    }
  }


}
