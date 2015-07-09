package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.{Login, ZuoraApi}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.SubscriptionData
import org.joda.time.Period
import services.zuora.Subscribe
import utils.ScheduledTask

import scala.concurrent.Future
import scala.concurrent.duration._

class ZuoraService(zuoraApiConfig: ZuoraApiConfig) extends ZuoraApi {

  override val apiConfig: ZuoraApiConfig = zuoraApiConfig

  override implicit def authentication: Authentication = authTask.get()

  override val application: String = Config.appName
  override val stage: String = Config.stage

  override val metrics = new ZuoraMetrics(stage, application)
  val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(request(Login(apiConfig)))

  def createSubscription(memberId: MemberId, data: SubscriptionData, paymentDelay: Option[Period]): Future[SubscribeResult] = {
    request(Subscribe(memberId, data, paymentDelay))
  }
}

