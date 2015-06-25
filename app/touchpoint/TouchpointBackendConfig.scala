package touchpoint

import com.gu.membership.salesforce.SalesforceConfig
import com.gu.membership.zuora.ZuoraApiConfig
import com.typesafe.scalalogging.LazyLogging

case class TouchpointBackendConfig(salesforce: SalesforceConfig, zuora: ZuoraApiConfig)

object TouchpointBackendConfig extends LazyLogging {

  sealed abstract class BackendType(val name: String)

  object BackendType {

    object Default extends BackendType("default")

    object Testing extends BackendType("test")

  }

  def byType(typ: BackendType = BackendType.Default, config: com.typesafe.config.Config) = {
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
      ZuoraApiConfig.forSubscriptions(envBackendConf, environmentName)
    )
  }
}