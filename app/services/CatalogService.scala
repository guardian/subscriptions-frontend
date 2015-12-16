package services

import com.gu.monitoring.ServiceMetrics
import com.gu.zuora.{ZuoraSoapConfig, soap}
import configuration.Config
import model.zuora.{SubscriptionProduct, DigitalProductPlan}
import monitoring.TouchpointBackendMetrics
import play.api.Play.current
import play.api.libs.concurrent.Akka

import scala.concurrent.Future
import scala.concurrent.duration._


class CatalogService(zuoraSoapConfig: ZuoraSoapConfig,
                     digitalProductPlan: DigitalProductPlan) {

  private val akkaSystem = Akka.system
  private val client = new soap.Client(zuoraSoapConfig, new ServiceMetrics(Config.stage, Config.appName, "zuora-soap-client"), akkaSystem)
  private val cache: ProductsCache = new ProductsCache(client, akkaSystem, digitalProductPlan, new TouchpointBackendMetrics {
    val backendEnv = zuoraSoapConfig.envName
    val service = "ProductsCache"
  }).refreshEvery(2.hour)

  def products: Future[Seq[SubscriptionProduct]] = cache.items
}
