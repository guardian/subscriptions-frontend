package monitoring

import com.amazonaws.services.cloudwatch.model.Dimension
import configuration.Config

//todo move to mem common
trait TouchpointBackendMetrics extends Metrics {
  val backendEnv: String

  val standardBackend = Config.stage == backendEnv

  override def mandatoryDimensions = if (standardBackend) super.mandatoryDimensions else {
    super.mandatoryDimensions :+ new Dimension().withName("Backend").withValue(backendEnv)
  }
}
