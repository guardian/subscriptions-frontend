package actions

import configuration.Config
import controllers.routes
import com.gu.googleauth

object OAuthActions extends googleauth.Actions with googleauth.Filters {
  val authConfig  = Config.googleAuthConfig
  val loginTarget = routes.OAuth.loginAction()
  lazy val groupChecker = Config.googleGroupChecker
}
