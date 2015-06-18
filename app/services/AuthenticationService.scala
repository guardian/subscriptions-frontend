package services

import com.gu.identity.cookie.IdentityKeys
import configuration.Config

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  override def idWebAppSigninUrl(returnUrl: String): String = "todo"

  override val identityKeys: IdentityKeys = Config.Identity.keys
}