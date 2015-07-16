package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.salesforce.{BasicMember, MemberId}
import com.gu.membership.zuora.soap.Zuora.{Authentication, SubscribeResult}
import model.zuora.SubscriptionProduct
import model.{PaymentData, PersonalData, SubscriptionData}
import org.scalatest.FreeSpec
import services.CheckoutService.CheckoutResult
import utils.ScheduledTask
import utils.TestPersonalData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class CheckoutServiceSpec extends FreeSpec {

  def makeIdentityService(onUserUpdate: => Unit = {}): IdentityService=
    new IdentityService(???) {
      override def registerGuest(personalData: PersonalData): Future[GuestUser] = {
        Future { GuestUser(UserId(personalData.firstName), IdentityToken("token")) }
      }

      override def updateUserDetails(personalData: PersonalData, userId: UserId, authCookie: AuthCookie): Future[Unit] = {
        Future { onUserUpdate }
      }
    }

  object TestSalesforceService extends SalesforceService {
    override def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[MemberId] =
      Future { BasicMember(s"${userId.id} contactId", s"${userId.id} accountId") }
  }

  object TestZuoraService extends ZuoraService {
    override def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult] = {
      Future { SubscribeResult(
        id = s"Subscribed ${memberId.salesforceContactId}", name = "A-Sxxxxx") }
    }

    override def authTask: ScheduledTask[Authentication] = ???

    override def productsTask: ScheduledTask[Seq[SubscriptionProduct]] = ???

    override def products: Seq[SubscriptionProduct] = ???
  }

  "processSubscription" - {
    val subscriptionData = SubscriptionData(testPersonalData.copy(firstName = "Registered"), PaymentData("", "", "", "", ""), "")
    val service = new CheckoutService(makeIdentityService(), TestSalesforceService, TestZuoraService)

    "for a registered user" - {
      def process(service: CheckoutService): Future[CheckoutResult] =
        service.processSubscription(
          subscriptionData,
          Some(IdMinimalUser("RegisteredId", None)),
          Some(AuthCookie("cookie")))

      val checkoutResult = Await.result(
        process(service),
        2.seconds
      )

      "returns the checkout result" - {
        "containing the Salesforce memberId" in {
          assertResult(BasicMember("RegisteredId contactId", "RegisteredId accountId"))(checkoutResult.salesforceMember)
        }

        "containing the Zuora subscription result" in {
          assertResult(SubscribeResult("Subscribed RegisteredId contactId", "A-Sxxxxx"))(checkoutResult.zuoraResult)
        }
      }

      "updates the user details in the Identity service" in {
        var updated = false

        val service = new CheckoutService(
          makeIdentityService { updated = true },
          TestSalesforceService, TestZuoraService
        )

        val checkoutResult = Await.result(
          process(service),
          2.seconds
        )

        assert(updated)
      }

    }

    "for a guest user" - {
      def process(service: CheckoutService): Future[CheckoutResult] =
        service.processSubscription(subscriptionData, None, None)

      val checkoutResult = Await.result(
        process(service),
        2.seconds
      )

      "does not update the user details in the Identity service" in {
        var updated = false

        val service = new CheckoutService(
          makeIdentityService { updated = true },
          TestSalesforceService, TestZuoraService
        )

        val checkoutResult = Await.result(
          process(service),
          2.seconds
        )

        assert(!updated)
      }
    }
  }
}
