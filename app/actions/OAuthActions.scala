package actions

import configuration.Config
import controllers.routes

trait OAuthActions extends com.gu.googleauth.Actions {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()
}

object OAuthActions extends OAuthActions

