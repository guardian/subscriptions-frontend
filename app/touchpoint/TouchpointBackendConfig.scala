package touchpoint

import com.gu.i18n.Country
import com.gu.salesforce.SalesforceConfig
import com.gu.stripe.StripeServiceConfig
import com.gu.zuora.api.{InvoiceTemplate, InvoiceTemplates}
import com.gu.zuora.{ZuoraApiConfig, ZuoraRestConfig, ZuoraSoapConfig}
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.Days

case class TouchpointBackendConfig(
    environmentName: String,
    salesforce: SalesforceConfig,
    zuoraSoap: ZuoraSoapConfig,
    zuoraRest: ZuoraRestConfig,
    zuoraProperties: ZuoraProperties,
    stripeUK: StripeServiceConfig,
    stripeAU: StripeServiceConfig,
    goCardlessToken: GoCardlessToken
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
      StripeServiceConfig.from(envBackendConf, environmentName, Country.UK),
      StripeServiceConfig.from(envBackendConf, environmentName, Country.Australia, "au-membership"),
      GoCardlessToken.from(envBackendConf, environmentName)
    )
  }
}

object ZuoraProperties {
  def from(config: com.typesafe.config.Config, environmentName: String): ZuoraProperties = {
    ZuoraProperties(
      Days.days(config.getInt("zuora.paymentDelayInDays")),
      Days.days(config.getInt("zuora.paymentDelayGracePeriod")),
      InvoiceTemplates.fromConfig(config.getConfig("zuora.invoiceTemplateIds"))
    )
  }
}
case class ZuoraProperties(
  defaultDigitalPackFreeTrialPeriod: Days,
  gracePeriodInDays: Days,
  invoiceTemplates: List[InvoiceTemplate]
)

object GoCardlessToken {
  def from(config: com.typesafe.config.Config, environmentName: String): GoCardlessToken = {
    GoCardlessToken(
      token = config.getString("gocardless.token"),
      isProdToken = environmentName == "PROD"
    )
  }
}
case class GoCardlessToken(token: String, isProdToken: Boolean)
