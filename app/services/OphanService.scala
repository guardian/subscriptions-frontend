package services

import com.gu.okhttp.RequestRunners

object OphanService {

  private val prodService = com.gu.acquisition.services.OphanService.prod(RequestRunners.client)

  def apply(isTestService: Boolean): com.gu.acquisition.services.OphanService =
    if (isTestService) {
      com.gu.acquisition.services.MockOphanService
    } else {
      prodService
    }
}
