package touchpoint

import com.gu.membership.salesforce.SalesforceConfig
import com.gu.membership.zuora.ZuoraApiConfig
import com.typesafe.scalalogging.LazyLogging
import model.zuora.DigitalProductPlan

case class TouchpointBackendConfig(salesforce: SalesforceConfig, zuora: ZuoraApiConfig, digitalProductPlan: DigitalProductPlan)

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
      DigitalProductPlan(envBackendConf.getString("zuora.digital"))
    )
  }
}
