package services

import com.gu.acquisition.services.{Ec2OrLocalConfig, MockAcquisitionService}
import com.gu.memsub.auth.common.MemSub.AWSCredentialsProvider
import com.gu.okhttp.RequestRunners
import configuration.Config

object AcquisitionService {

  private val prodService = com.gu.acquisition.services.AcquisitionService.prod(Ec2OrLocalConfig(AWSCredentialsProvider.Chain, Config.kinesisStream))(RequestRunners.client)

  def apply(environmentName: String): com.gu.acquisition.services.AcquisitionService =
    if(environmentName == "DEV" && !Config.analyticsOnInDev) {
      MockAcquisitionService
    } else {
      prodService
    }
}
