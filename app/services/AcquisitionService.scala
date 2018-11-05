package services

import com.gu.acquisition.services.MockAcquisitionService
import com.gu.okhttp.RequestRunners
import configuration.Config

object AcquisitionService {

  private val prodService = com.gu.acquisition.services.AcquisitionService.prod(RequestRunners.client)

  def apply(environmentName: String): com.gu.acquisition.services.AcquisitionService =
    if(environmentName == "DEV" && !Config.analyticsOnInDev) {
      MockAcquisitionService
    } else {
      prodService
    }
}
