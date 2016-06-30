package services
import views.support.Pricing._
import com.gu.i18n.Currency
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo._
import com.gu.memsub.services.{SubscriptionService, PaymentService => CommonPaymentService}
import com.gu.memsub.util.ScheduledTask
import com.gu.memsub._
import com.gu.subscriptions.{DigipackCatalog, PaperCatalog}
import com.gu.zuora.soap.models.Results.SubscribeResult
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody, Response}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.SubscribeRequest
import model.error.ExactTragetService.ExactTargetAuthenticationError
import model.exactTarget.SubscriptionDataExtensionRow
import org.joda.time.Days
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._

trait ExactTargetService extends LazyLogging {
  lazy val etClient: ETClient = ETClient

  def digiSubscriptionService: SubscriptionService[DigipackCatalog]
  def paperSubscriptionService: Option[SubscriptionService[PaperCatalog]]
  def paymentService: CommonPaymentService

  private def getPlanDescription(validPromotion: Option[ValidPromotion[NewUsers]], currency: Currency, plan: PaidPlan[Status, BillingPeriod]): String = {
    (for {
      vp <- validPromotion
      discountPromotion <- vp.promotion.asDiscount
    } yield {
      plan.prettyPricingForDiscountedPeriod(discountPromotion, currency)
    }).getOrElse(plan.prettyPricing(currency))
  }

  def sendETDataExtensionRow(subscribeResult: SubscribeResult, subscriptionData: SubscribeRequest, gracePeriod: Days, validPromotion: Option[ValidPromotion[NewUsers]]): Future[Unit] = {

    val subscription = subscriptionData.productData.fold({ paper =>
      paperSubscriptionService.get.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName))
    }, {digipack =>
      digiSubscriptionService.unsafeGetPaid(Subscription.Name(subscribeResult.subscriptionName))
    })

    val paymentMethod = paymentService.getPaymentMethod(Subscription.AccountId(subscribeResult.accountId)).map(
      _.getOrElse(throw new Exception(s"Subscription with no payment method found, ${subscribeResult.subscriptionId}"))
    )

    val promotionDescription = validPromotion.filterNot(_.promotion.promotionType == Tracking).map(_.promotion.description)

    for {
      sub <- subscription
      pm <- paymentMethod
      row = SubscriptionDataExtensionRow(
        personalData = subscriptionData.genericData.personalData,
        subscription = sub,
        paymentMethod = pm,
        gracePeriod = gracePeriod,
        subscriptionDetails = getPlanDescription(validPromotion, subscriptionData.genericData.personalData.currency, sub.plan),
        promotionDescription = promotionDescription
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

  implicit val as = Akka.system
  val task = ScheduledTask("emails", "", 0.seconds, 30.minutes)(Future {
    getAccessToken
  })
  task.start()


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
                          .header("Authorization", s"Bearer ${task.get()}")
                          .build()
      val response = httpClient.newCall(request).execute()

      response
    }

  }

  override def forceAccessTokenRefresh() {}
}
