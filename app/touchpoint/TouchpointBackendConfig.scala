package touchpoint

import com.github.nscala_time.time.Imports._
import com.gu.salesforce.SalesforceConfig
import com.gu.stripe.StripeApiConfig
import com.gu.zuora.{ZuoraRestConfig, ZuoraSoapConfig, ZuoraApiConfig}
import com.typesafe.scalalogging.LazyLogging
import model.zuora.DigitalProductPlan
import org.joda.time.Period

case class TouchpointBackendConfig(
  environmentName: String,
  salesforce: SalesforceConfig,
  zuoraSoap: ZuoraSoapConfig,
  zuoraRest: ZuoraRestConfig,
  zuoraProperties: ZuoraProperties,
  digitalProductPlan: DigitalProductPlan,
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
      DigitalProductPlan(envBackendConf.getString("zuora.digital")),
      StripeApiConfig.from(envBackendConf, environmentName)
    )
  }
}

object ZuoraProperties {
  def from(config: com.typesafe.config.Config, environmentName: String) = {
    ZuoraProperties(
      config.getInt("zuora.paymentDelayInDays").days
    )
  }
}
case class ZuoraProperties(
  paymentDelayInDays: Period
)
