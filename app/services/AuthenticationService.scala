package services

import com.gu.identity.cookie.IdentityKeys
import com.gu.identity.play.AuthenticationService
import configuration.Config

object AuthenticationService extends AuthenticationService {
  override def idWebAppSigninUrl(returnUrl: String): String = "todo"

  override val identityKeys: IdentityKeys = Config.Identity.keys
}