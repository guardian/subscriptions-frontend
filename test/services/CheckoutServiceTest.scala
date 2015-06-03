package services

import com.gu.membership.salesforce.{BasicMember, MemberId}
import model.{AddressData, PaymentData, SubscriptionData, PersonalData}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import testUtils.FutureUtils.await
import testUtils.{FutureUtils, FakeWSResponse}

import scala.concurrent.Future

class CheckoutServiceTest extends FlatSpec with Matchers {

  val identityApiClient: IdentityApiClient = new IdentityApiClient {
    override def createGuest: (JsValue) => Future[WSResponse] = _ => Future.successful(FakeWSResponse())

    override def userLookupByScGuUCookie: (String) => Future[WSResponse] = _ => Future.successful(FakeWSResponse())

    override def userLookupByEmail: (String) => Future[WSResponse] = _ => Future.successful(FakeWSResponse())
  }

  private val salesforceService: SalesforceService = new SalesforceService {
    override def createSFUser(personalData: PersonalData, idUser: IdUser): Future[MemberId] = Future.successful(BasicMember("", ""))
  }

  val subject = new CheckoutService(new IdentityService(identityApiClient), salesforceService)

  val subscriptionData: SubscriptionData = SubscriptionData(
    PersonalData("", "", "", AddressData("", "", "", "")),
    PaymentData("", 1, 2, 3, ""))

  "Process subscription" should "succeed SC_GU_U cookie is provided" in {
    await(subject.processSubscription(subscriptionData, Some("valid SC_GU_U"))) should be(Right("AB123456"))
  }

}

