package touchpoint

import com.gu.membership.salesforce.SalesforceConfig
import com.gu.membership.zuora.ZuoraApiConfig
import com.typesafe.scalalogging.LazyLogging
import touchpoint.BillingFrequency.{Annual, Quarterly, Monthly}
import touchpoint.ProductPlan.Digital

case class TouchpointBackendConfig(salesforce: SalesforceConfig, zuora: ZuoraApiConfig, productRatePlan: Seq[ProductRatePlan])

object TouchpointBackendConfig extends LazyLogging {

  sealed abstract class BackendType(val name: String)

  object BackendType {

    object Default extends BackendType("default")

    object Testing extends BackendType("test")

  }

  def backendType(typ: BackendType = BackendType.Default, config: com.typesafe.config.Config) = {
    val backendsConfig = config.getConfig("touchpoint.backend")
    val environmentName = backendsConfig.getString(typ.name)

    val touchpointBackendConfig = byEnv(environmentName, backendsConfig)

    logger.info(s"TouchPoint config - $typ: config=${touchpointBackendConfig.hashCode}")

    touchpointBackendConfig
  }

  def byEnv(environmentName: String, backendsConfig: com.typesafe.config.Config) = {
    val envBackendConf = backendsConfig.getConfig(s"environments.$environmentName")

    TouchpointBackendConfig(
      SalesforceConfig.from(envBackendConf, environmentName),
      ZuoraApiConfig.from(envBackendConf, environmentName),
      ProductRatePlan(envBackendConf)
    )
  }
}

sealed abstract class ProductPlan
object ProductPlan {
  case object Digital extends ProductPlan
}

sealed abstract class BillingFrequency
object BillingFrequency {
  case object Monthly extends BillingFrequency
  case object Quarterly extends BillingFrequency
  case object Annual extends BillingFrequency
}


case class ProductRatePlan(
  product: ProductPlan,
  frequency: BillingFrequency,
  ratePlanId: String
)

object ProductRatePlan {
  def apply(config: com.typesafe.config.Config): Seq[ProductRatePlan] = {
    val digitalConfig = config.getConfig("zuora.digital")
    Seq(
      ProductRatePlan(Digital, Monthly , digitalConfig.getString("monthly")),
      ProductRatePlan(Digital, Quarterly, digitalConfig.getString("quarterly")),
      ProductRatePlan(Digital, Annual, digitalConfig.getString("annual"))
    )
  }
}

