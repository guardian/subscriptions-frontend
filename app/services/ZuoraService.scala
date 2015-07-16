package services

import com.gu.membership.salesforce.MemberId
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap.ZuoraDeserializer._
import com.gu.membership.zuora.soap.{Login, ZuoraApi}
import com.gu.monitoring.ZuoraMetrics
import configuration.Config
import model.SubscriptionData
import services.zuora.Subscribe
import touchpoint.ProductRatePlan
import utils.ScheduledTask

import scala.concurrent.Future
import scala.concurrent.duration._

trait ZuoraService {
  def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult]
  def authTask:ScheduledTask[Authentication]
}

class ZuoraApiClient(zuoraApiConfig: ZuoraApiConfig, productRatePlan: ProductRatePlan) extends ZuoraApi with ZuoraService {
  override implicit def authentication: Authentication = authTask.get()

  override val apiConfig = zuoraApiConfig
  override val application = Config.appName
  override val stage = Config.stage

  override val metrics = new ZuoraMetrics(stage, application)
  override val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(request(Login(apiConfig)))

  override def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult] = {
    request(Subscribe(memberId, data, productRatePlan.ratePlanId))
  }

}
