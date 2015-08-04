package services

import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.membership.salesforce.{BasicMember, MemberId}
import com.gu.membership.zuora.soap.Zuora.{Authentication, SubscribeResult}
import model.zuora.SubscriptionProduct
import model.{PaymentData, PersonalData, SubscriptionData}
import org.scalatest.FreeSpec
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import utils.ScheduledTask
import utils.TestPersonalData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckoutServiceSpec extends FreeSpec with Futures with ScalaFutures {
  implicit override val patienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)), interval = scaled(Span(50, Millis)))

  class UpdateFlag(var updated: Boolean = false)

  def makeIdentityService(updateFlag: UpdateFlag): IdentityService=
    new IdentityService(???) {
      override def registerGuest(personalData: PersonalData): Future[GuestUser] = {
        Future { GuestUser(UserId(personalData.firstName), IdentityToken("token")) }
      }

      override def updateUserDetails(personalData: PersonalData, userId: AuthenticatedIdUser): Future[Unit] = {
        Future { updateFlag.updated = true }
      }
    }

  object TestSalesforceService extends SalesforceService {
    override def repo = ???
    override def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[MemberId] =
      Future { BasicMember(s"${userId.id} contactId", s"${userId.id} accountId") }
  }

  object TestZuoraService extends ZuoraService {
    override def createSubscription(memberId: MemberId, data: SubscriptionData): Future[SubscribeResult] = {
      Future { SubscribeResult(
        id = s"Subscribed ${memberId.salesforceContactId}", name = "A-Sxxxxx") }
    }

    override def authTask: ScheduledTask[Authentication] = ???

    override def products: Seq[SubscriptionProduct] = Seq.empty
  }


  "processSubscription" - {
    val subscriptionData = SubscriptionData(testPersonalData.copy(firstName = "Registered"), PaymentData("", "", "", "", ""), "")
    val updateFlag = new UpdateFlag()
    val service = new CheckoutService(
      makeIdentityService(updateFlag), TestSalesforceService, TestZuoraService
    )

    "for a registered user" - {
      val checkoutResult = service.processSubscription(
        subscriptionData,
        Some(AuthenticatedIdUser("cookie", IdMinimalUser("RegisteredId", None)))
      )

      whenReady(checkoutResult) { res =>
        "returns the checkout result" - {
          "containing the Salesforce memberId" in {
            assertResult(BasicMember("RegisteredId contactId", "RegisteredId accountId"))(res.salesforceMember)
          }

          "containing the Zuora subscription result" in {
            assertResult(SubscribeResult("Subscribed RegisteredId contactId", "A-Sxxxxx"))(res.zuoraResult)
          }
        }
      }
    }

    "for a guest user" - {
      val updateFlag = new UpdateFlag()
      val service = new CheckoutService(
        makeIdentityService(updateFlag), TestSalesforceService, TestZuoraService
      )

      whenReady(service.processSubscription(subscriptionData, None)) { _ =>
        "does not update the user details in the Identity service" in {
          assert(!updateFlag.updated)
        }
      }
    }
  }
}
