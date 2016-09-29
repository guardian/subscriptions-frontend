package monitoring

import com.amazonaws.services.cloudwatch.model.Dimension
import com.gu.monitoring.ServiceMetrics
import configuration.Config

trait TouchpointBackendMetrics extends ServiceMetrics {
  val backendEnv: String

  val standardBackend = Config.stage == backendEnv

  override def mandatoryDimensions = if (standardBackend) super.mandatoryDimensions else {
    super.mandatoryDimensions :+ new Dimension().withName("Backend").withValue(backendEnv)
  }
}
