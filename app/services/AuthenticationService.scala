package services

import com.gu.identity.cookie.IdentityKeys
import com.gu.identity.play.AuthenticationService
import configuration.Config

object AuthenticationService extends AuthenticationService {
  override def idWebAppSigninUrl(returnUrl: String): String = Config.Identity.webAppSigninUrl(returnUrl)

  override val identityKeys: IdentityKeys = Config.Identity.keys
}