package utils

import com.gu.lib.Retry
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan._
import com.gu.salesforce.Contact
import com.gu.zuora.api.ZuoraService
import services.SalesforceService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

object GetSalesforceContactForSub {

  def apply(subscription: Subscription[AnyPlan])(implicit zuoraService: ZuoraService, salesforceService: SalesforceService): Future[Contact] = {
    for {
      zuoraAccount <- Retry(2, s"Failed to get Zuora account for subscription: ${subscription.name.get}") {
        zuoraService.getAccount(subscription.accountId)
      }
      sfContactId <- zuoraAccount.sfContactId.fold[Future[String]](Future.failed(new IllegalStateException(s"Zuora record for ${subscription.accountId} has no sfContactId")))(Future.successful)
      salesforceContact <- Retry(2, s"Failed to get Salesforce Contact for sfContactId: $sfContactId") {
        salesforceService.repo.getByContactId(sfContactId).map(_.getOrElse(throw new IllegalStateException(s"Cannot find salesforce contact for $sfContactId")))
      }
    } yield salesforceContact
  }

}
