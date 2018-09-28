package services

import com.gu.okhttp.RequestRunners

object AcquisitionService {

  private val prodService = com.gu.acquisition.services.AcquisitionService.prod(RequestRunners.client)

  def apply(isTestService: Boolean): com.gu.acquisition.services.AcquisitionService =
    if (isTestService) {
      com.gu.acquisition.services.MockAcquisitionService
    } else {
      prodService
    }
}
