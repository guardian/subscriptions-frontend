package model.error

object ExactTragetService {
  class ExactTargetAuthenticationError(msg: String) extends Exception(msg)
}
