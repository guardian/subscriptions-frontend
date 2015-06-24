package touchpoint

import com.gu.membership.model.ProductRatePlan
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._

object ZuoraApiConfig {
  def from(config: com.typesafe.config.Config, environmentName: String) = {

    ZuoraApiConfig(
      environmentName,
      config.getString("zuora.api.url"),
      username = config.getString("zuora.api.username"),
      password = config.getString("zuora.api.password"),
      productRatePlans = Map()
    )
  }
}

case class ZuoraApiConfig(envName: String, url: Uri, username: String, password: String, productRatePlans: Map[ProductRatePlan, String])

