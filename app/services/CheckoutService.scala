package services

import java.util.NoSuchElementException

import com.typesafe.scalalogging.LazyLogging
import model.SubscriptionData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Left

class CheckoutService(identityService: IdentityService, salesforceService: SalesforceService) extends LazyLogging {

  sealed class CheckoutException extends RuntimeException
  object GuestUserNotCreated extends CheckoutException
  object InvalidLoginCookie extends CheckoutException
  object SalesforceUserNotCreated extends CheckoutException

  def processSubscription(subscriptionData: SubscriptionData, scGuUCookie: Option[String] = None): Future[Either[CheckoutException, String]] = {

    val preserveFailureReason = (ex: CheckoutException) => Future.successful(Left(ex))

    val lookupUser: Future[Either[CheckoutException, IdUser]] =
      scGuUCookie.map(
        identityService.userLookupByScGuU(_)
          .map(_.toRight(InvalidLoginCookie)))
        .orElse(
          Some(identityService.userLookupByEmail(subscriptionData.personalData.email)
            .filter(_.isDefined)
            .recoverWith { case _: NoSuchElementException => identityService.registerGuest(subscriptionData.personalData) }
            .map(_.toRight(GuestUserNotCreated))))
        .get

    def createSFUser(idUser: Either[CheckoutException, IdUser]) = idUser.fold(preserveFailureReason,
      salesforceService.createSFUser(subscriptionData.personalData, _)
        .map(Right(_)).recoverWith {
          case e: Throwable =>
            logger.error("Could not create Salesforce user", e)
            Future.successful(Left(SalesforceUserNotCreated))
        })

    for {
      idUser <- lookupUser
      sfUser <- createSFUser(idUser)
    } yield {
      println(s">>>userID:$idUser")
      println(s">>>sfUser:$sfUser")
      Right("AB123456")
    }
  }

}

object CheckoutService extends CheckoutService(IdentityService, SalesforceService)