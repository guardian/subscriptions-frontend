package services

import com.gu.identity.cookie.IdentityKeys
import com.gu.identity.play.AuthenticatedIdUser
import configuration.Config
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  override val identityKeys: IdentityKeys = Config.Identity.keys
}

class AsyncAuthenticationService(implicit ec: ExecutionContext) {

  def authenticatedUserFor(requestHeader: RequestHeader): Future[AuthenticatedIdUser] =
    AuthenticationService.authenticatedUserFor(requestHeader) match {
      case Some(user) => Future.successful(user)
      case None => Future.failed(new RuntimeException("unable to authenticate user"))
    }

  def tryAuthenticatedUserFor(requestHeader: RequestHeader): Future[Option[AuthenticatedIdUser]] =
    authenticatedUserFor(requestHeader)
      .map(user => Option(user))
      .recover { case _ => None }
}
