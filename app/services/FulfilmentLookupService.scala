package services

import com.gu.okhttp.RequestRunners.futureRunner
import configuration.Config
import forms.DeliveryIssue
import okhttp3.{MediaType, RequestBody, Response}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object FulfilmentLookupService {

  val httpClient = futureRunner

  def buildRequest(environment: String, deliveryIssue: DeliveryIssue): okhttp3.Request = {
    val apiDateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    val formattedDate = apiDateFormatter.print(deliveryIssue.issueDate)
    val body = RequestBody.create(MediaType.parse("application/json"), Json.obj("subscriptionName" -> deliveryIssue.subscriptionName.get, "issueDate" -> formattedDate).toString())
    new okhttp3.Request.Builder()
      .addHeader("x-api-key", Config.fulfilmentLookupApiKey)
      .url(s"${Config.fulfilmentLookupApiUrl}/fulfilment-lookup")
      .post(body)
      .build()
  }

  def lookupSubscription(environment: String, deliveryIssue: DeliveryIssue): Future[Response] = {
    val request = buildRequest(environment, deliveryIssue)
    httpClient(request)
  }


}
