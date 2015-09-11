package services

import akka.agent.Agent
import com.gu.membership.zuora.soap.models.Result.SubscribeResult
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{MediaType, OkHttpClient, RequestBody, Response}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.SubscriptionData
import model.exactTarget.{ExactTargetException, SubscriptionDataExtensionRow}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration._

trait ExactTargetService extends LazyLogging {
  def etClient: ETClient

  def sendETDataExtensionRow(subscribeResult: SubscribeResult, subscriptionData: SubscriptionData, zs: ZuoraService): Future[Unit] = {
    val subscription = zs.subscriptionByName(subscribeResult.name)

    val accAndPaymentMethod = for {
      subs <- subscription
      acc <- zs.account(subs)
      pm <- zs.defaultPaymentMethod(acc)
    } yield (acc, pm)

    for {
      subs <- subscription
      rpc <- zs.normalRatePlanCharge(subs)
      (acc, pm) <- accAndPaymentMethod
      row = SubscriptionDataExtensionRow(
        subscription = subs,
        subscriptionData = subscriptionData,
        ratePlanCharge = rpc,
        paymentMethod = pm,
        account = acc
      )
      response <- etClient.sendSubscriptionRow(row)
    } yield {
      response.code() match {
        case 202 => {
          logger.info(s"Successfully sent an email to confirm the subscription: $subscribeResult")
        }
        case _ => {
          val errorMsg = s"Failed to send the subscription email $subscribeResult. Code: ${response.code()}, Message: ${response.body()}"
          logger.error(errorMsg)
          throw new ExactTargetException(errorMsg)
        }
      }
    }
  }
}

object ExactTargetService extends ExactTargetService {
  lazy val etClient = ETClient
}

trait ETClient {
  def sendSubscriptionRow(row: SubscriptionDataExtensionRow): Future[Response]
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
    val payload = Json.obj(
      "clientId" -> clientId,
      "clientSecret" -> clientSecret
    ).toString()

    val body = RequestBody.create(jsonMT, payload)
    val request = new Builder().url(authEndpoint).post(body).build()
    val response = httpClient.newCall(request).execute()

    val respBody = response.body().string()

    logger.info("Got new token: " + (Json.parse(respBody) \ "accessToken").as[String])
    (Json.parse(respBody) \ "accessToken").as[String]
  }

  Akka.system.scheduler.schedule(initialDelay = 55.minutes, interval = 55.minutes) {
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
}
