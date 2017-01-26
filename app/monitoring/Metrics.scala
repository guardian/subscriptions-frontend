package monitoring

import com.gu.monitoring.CloudWatch
import configuration.Config

trait Metrics extends CloudWatch {
  val stage = Config.stage
  val application = Config.appName
}
