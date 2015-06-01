package services

import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData
import services.IdentityService.IdUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left

object CheckoutService extends LazyLogging {

  sealed class CheckoutException extends RuntimeException
  object LoginRequired extends CheckoutException
  object InvalidLoginCookie extends CheckoutException
  object SalesforceUserNotCreated extends CheckoutException

  def processSubscription(subscriptionData: SubscriptionData, scGuUCookie:Option[String] = None): Future[Either[CheckoutException,String]] = {

    def eventualFailureReason = (ex: CheckoutException) => Future.successful(Left(ex))

    val lookupUser: Future[Either[CheckoutException, IdUser]] =
      scGuUCookie.map(
        IdentityService.userLookupByScGuU(_)
          .map(_.toRight(InvalidLoginCookie)))
        .orElse(
          Some(IdentityService.userLookupByEmail(subscriptionData.personalData.email)
            .map(_.toRight(LoginRequired))))
        .get

    lookupUser.onComplete{case x => println(s">>> lookupUser: $x")}

    def createSFUser = (idUser:Either[CheckoutException, IdUser]) => {
      println(s">>> createSFUser start")
      val x = idUser.fold(eventualFailureReason, SalesforceService.createSFUser(subscriptionData.personalData, _))
      println(s">>> createSFUser end")
      x
    }

    for {
      idUser <- lookupUser
      sfUser <- createSFUser(idUser)
    } yield {
      println(s">>>userID:$idUser")
      println(s">>>sfUser:$sfUser")
    }

    Future.successful(Right("AB123456"))
  }

}
