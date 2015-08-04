package services

import com.gu.identity.cookie.IdentityKeys
import configuration.Config

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  def idWebAppSigninUrl(returnUrl: String): String = Config.Identity.webAppSigninUrl(returnUrl)

  override val identityKeys: IdentityKeys = Config.Identity.keys
}
