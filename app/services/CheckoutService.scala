package services

import model.SubscriptionData
import services.IdentityService.IdUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CheckoutService {

  sealed class CheckoutException extends RuntimeException
  object LoginRequired extends CheckoutException
  object InvalidLoginCookie extends CheckoutException

  def processSubscription(subscriptionData: SubscriptionData, scGuUCookie:Option[String] = None): Future[Either[CheckoutException,String]] = {

    val userId: Future[Either[CheckoutException, IdUser]] = scGuUCookie.map {
      IdentityService.userLookupByScGuU(_)
        .map(_.toRight(InvalidLoginCookie))
    }.orElse {
      Some(IdentityService.userLookupByEmail(subscriptionData.personalData.email)
        .map(_.toRight(LoginRequired)))
    }.get

    for {
      userId <- userId 
    } yield println(s">>>userID:$userId")

    Future.successful(Right("AB123456"))
  }
}
