package model.error

import services.UserIdData

object IdentityService {

  sealed trait IdentityResult
  case class IdentitySuccess(userData: UserIdData) extends IdentityResult
  case class IdentityFailure(msg: String,
                             requestData: String,
                             errorResponse: Option[String]) extends SubsError with IdentityResult {
    override val message = msg
    override val request = requestData
    override val response = errorResponse
  }

}
