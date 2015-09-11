package services

import com.gu.identity.play.{AuthenticatedIdUser, IdMinimalUser}
import com.gu.membership.salesforce.{BasicMember, MemberId}
import com.gu.membership.zuora.soap.models.Query._
import com.gu.membership.zuora.soap.models.Result.SubscribeResult
import com.squareup.okhttp.Response
import model.exactTarget.SubscriptionDataExtensionRow
import model.zuora.SubscriptionProduct
import model.{PaymentData, PersonalData, SubscriptionData, SubscriptionRequestData}
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
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
    override def createSubscription(memberId: MemberId, data: SubscriptionData, requestData: SubscriptionRequestData): Future[SubscribeResult] = {
      Future { SubscribeResult(
        id = s"Subscribed ${memberId.salesforceContactId}", name = "A-Sxxxxx") }
    }

    override def subscriptionByName(id: String): Future[Subscription] = {
      val date = new DateTime()
      Future {
         Subscription("test","test","213",1,date.plusDays(1),date,date, None)
      }
    }

    override  def ratePlans(subscription: Subscription): Future[Seq[RatePlan]] = ???
    def defaultPaymentMethod(account: Account): Future[PaymentMethod] = ???
    def account(subscription: Subscription): Future[Account] = ???
    def normalRatePlanCharge(subscription: Subscription): Future[RatePlanCharge] = ???

    override def products: Future[Seq[SubscriptionProduct]] = Future { Seq.empty }
  }

  object TestExactTargetService extends ExactTargetService {
    object TestETClient extends ETClient {
      override def sendSubscriptionRow(row: SubscriptionDataExtensionRow): Future[Response] = {
        Future.successful {
          new Response.Builder().build()
        }
      }
    }
    override def etClient: ETClient = TestETClient
  }


  "processSubscription" - {
    val subscriptionData = SubscriptionData(testPersonalData.copy(firstName = "Registered"), PaymentData("", "", ""), "")
    val updateFlag = new UpdateFlag()
    val service = new CheckoutService(
      makeIdentityService(updateFlag), TestSalesforceService, TestZuoraService, TestExactTargetService
    )

    "for a registered user" - {
      val checkoutResult = service.processSubscription(
        subscriptionData,
        Some(AuthenticatedIdUser("cookie", IdMinimalUser("RegisteredId", None))),
        SubscriptionRequestData("123.123.123.123")
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
        makeIdentityService(updateFlag), TestSalesforceService, TestZuoraService, TestExactTargetService
      )

      whenReady(service.processSubscription(subscriptionData, None, SubscriptionRequestData("123.123.123.123"))) { _ =>
        "does not update the user details in the Identity service" in {
          assert(!updateFlag.updated)
        }
      }
    }
  }
}
