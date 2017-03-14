package touchpoint

import com.gu.salesforce.SalesforceConfig
import com.gu.stripe.StripeApiConfig
import com.gu.zuora.{ZuoraRestConfig, ZuoraSoapConfig, ZuoraApiConfig}
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.Days

case class TouchpointBackendConfig(
  environmentName: String,
  salesforce: SalesforceConfig,
  zuoraSoap: ZuoraSoapConfig,
  zuoraRest: ZuoraRestConfig,
  zuoraProperties: ZuoraProperties,
  stripe: StripeApiConfig
)

object TouchpointBackendConfig extends LazyLogging {

  sealed abstract class BackendType(val name: String)

  object BackendType {

    object Default extends BackendType("default")

    object Testing extends BackendType("test")

    val All = Seq(Default, Testing)
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
      environmentName,
      SalesforceConfig.from(envBackendConf, environmentName),
      ZuoraApiConfig.soap(envBackendConf, environmentName),
      ZuoraApiConfig.rest(envBackendConf, environmentName),
      ZuoraProperties.from(envBackendConf, environmentName),
      StripeApiConfig.from(envBackendConf, environmentName)
    )
  }
}

object ZuoraProperties {
  def from(config: com.typesafe.config.Config, environmentName: String) = {
    ZuoraProperties(
      Days.days(config.getInt("zuora.paymentDelayInDays")),
      Days.days(config.getInt("zuora.paymentDelayGracePeriod"))
    )
  }
}
case class ZuoraProperties(
  defaultDigitalPackFreeTrialPeriod: Days,
  gracePeriodInDays: Days
)
