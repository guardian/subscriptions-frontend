package monitoring

import com.amazonaws.regions.{Region, Regions}
import com.gu.monitoring.CloudWatch
import configuration.Config

trait Metrics extends CloudWatch {
  val region = Region.getRegion(Regions.EU_WEST_1)
  val stage = Config.stage
  val application = "SubscriptionsFrontend"
}
