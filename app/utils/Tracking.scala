package utils

import play.api.mvc.RequestHeader

object Tracking {

  val internalCampaignCode = "INTCMP"

  val awinEnabled = false

  def awinEnabledForUser(implicit request: RequestHeader): Boolean =
    awinEnabled && request.cookies
      .get("GU_TK")
      .exists(!_.value.split('.').headOption.contains("0"))

}
