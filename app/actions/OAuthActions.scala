package actions

import configuration.Config
import controllers.routes

object OAuthActions extends com.gu.googleauth.Actions {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()
}