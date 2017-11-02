package services

import com.gu.okhttp.RequestRunners

object OphanService {

  def apply(isTestService: Boolean): com.gu.acquisition.services.OphanService =
    if (isTestService) {
      com.gu.acquisition.services.MockOphanService
    } else {
      com.gu.acquisition.services.OphanService.prod(RequestRunners.client)
    }
}
