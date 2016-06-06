package services

import akka.agent.Agent
import com.gu.memsub.{Digipack, Subscription}
import com.gu.memsub.services.SubscriptionService
import com.gu.subscriptions.DigipackCatalog
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody, Response}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.SubscriptionData
import model.exactTarget.SubscriptionDataExtensionRow
import model.error.ExactTragetService.ExactTargetAuthenticationError
import org.joda.time.Days
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.gu.memsub.services.{PaymentService => CommonPaymentService}
import play.api.libs.json._

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._

trait ExactTargetService extends LazyLogging {
  lazy val etClient: ETClient = ETClient

  def subscriptionService: SubscriptionService[DigipackCatalog]
  def paymentService: CommonPaymentService

  def sendETDataExtensionRow(subscribeResult: SubscribeResult, subscriptionData: SubscriptionData, gracePeriod: Days): Future[Unit] = {
      val subscription = subscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName))(Digipack)
      val paymentMethod = paymentService.getPaymentMethod(Subscription.AccountId(subscribeResult.accountId)).map(
        _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}"))
      )

    for {
      sub <- subscription
      pm <- paymentMethod
      row = SubscriptionDataExtensionRow(
        personalData = subscriptionData.personalData,
        subscription = sub,
        paymentMethod = pm,
        gracePeriod = gracePeriod
      )
      response <- etClient.sendSubscriptionRow(row)
    } yield {
      response.code() match {
        case 202 =>
          response.body().close()
          logger.info(s"Successfully sent ${subscribeResult.subscriptionName} welcome email.")
        case 401 =>
          logger.warn(s"Failed to send ${subscribeResult.subscriptionName} welcome email due to failed authorization. Refreshing access token and re-trying.")
          retrySendingEmail(row ,subscribeResult)
        case _ => logger.error(s"Failed to send ${subscribeResult.subscriptionName} welcome email. Code: ${response.code()}, Message: ${response.body.string()}")
      }
    }
  }

  private def retrySendingEmail(row: SubscriptionDataExtensionRow, subscribeResult: SubscribeResult) {
    etClient.forceAccessTokenRefresh()
    etClient.sendSubscriptionRow(row).map { response =>
      response.code() match {
        case 202 =>
          logger.info(s"Successfully sent ${subscribeResult.subscriptionName} welcome email.")
          response.body().close()
        case _ => logger.error(s"Failed to send ${subscribeResult.subscriptionName} welcome email. Code: ${response.code()}, Message: ${response.body.string()}")
      }
    }
  }
}

trait ETClient {
  def sendSubscriptionRow(row: SubscriptionDataExtensionRow): Future[Response]
  def forceAccessTokenRefresh()
}

object ETClient extends ETClient with LazyLogging {
  private val config = Config.ExactTarget
  private val clientId = config.clientId
  private val clientSecret = config.clientSecret
  lazy val welcomeTriggeredSendKey = Config.ExactTarget.welcomeTriggeredSendKey
  private val jsonMT = MediaType.parse("application/json; charset=utf-8")
  private val httpClient = new OkHttpClient()
  private val authEndpoint = "https://auth.exacttargetapis.com/v1/requestToken"
  private val restEndpoint = "https://www.exacttargetapis.com/messaging/v1"

  private val accessToken = Agent(getAccessToken)
  import play.api.Play.current

  /**
   * Get a token from Exact Target, that is expired every hour
   * See https://code.exacttarget.com/apis-sdks/rest-api/using-the-api-key-to-authenticate-api-calls.html
   * This call is blocking
   */
  private def getAccessToken: String = {

    @tailrec def repeater(count: Int): String =
      if (count == 0) {
        val errMsg = s"Error getting Exact Target access token. Welcome emails will not be sent."
        logger.error(errMsg)
        throw new ExactTargetAuthenticationError(errMsg)
      }
      else
        try {
          val payload = Json.obj("clientId" -> clientId, "clientSecret" -> clientSecret).toString()
          val body = RequestBody.create(jsonMT, payload)
          val request = new Builder().url(authEndpoint).post(body).build()
          val response = httpClient.newCall(request).execute()
          val respBody = response.body().string()
          val accessToken = (Json.parse(respBody) \ "accessToken").as[String]
          val expiresIn = (Json.parse(respBody) \ "expiresIn").as[Int]
          logger.info(s"Got new ExactTarget access token: ${accessToken.take(4)}... which expires in $expiresIn s")
          accessToken
        } catch {
          case e: Throwable =>
            logger.warn(s"Could not get ExactTarget access token: ${e.getMessage}")
            logger.warn(s"Trying again to get ExactTarget access token. Remaining attempts ... ${count-1}")
            repeater(count - 1)
        }

    repeater(3)
  }

  Akka.system.scheduler.schedule(initialDelay = 30.minutes, interval = 30.minutes) {
    accessToken send getAccessToken
  }

  /**
   * See https://code.exacttarget.com/apis-sdks/rest-api/v1/messaging/messageDefinitionSends.html
   */
  override def sendSubscriptionRow(row: SubscriptionDataExtensionRow): Future[Response] = {

    def endpoint = s"$restEndpoint/messageDefinitionSends/$welcomeTriggeredSendKey/send"

    Future {
      val payload = Json.obj(
        "To" -> Json.obj(
          "Address" -> row.email,
          "SubscriberKey" -> row.email,
          "ContactAttributes" -> Json.obj(
            "SubscriberAttributes" ->  Json.toJsFieldJsValueWrapper(row.fields.toMap)
          )
        )
      ).toString()

      val body = RequestBody.create(jsonMT, payload)
      val request = new Builder()
                          .url(endpoint)
                          .post(body)
                          .header("Authorization", s"Bearer ${accessToken.get()}")
                          .build()
      val response = httpClient.newCall(request).execute()

      response
    }

  }

  override def forceAccessTokenRefresh() { accessToken send getAccessToken }
}
