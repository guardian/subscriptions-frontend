package services

import model.{AddressData, PaymentData, PersonalData, SubscriptionData}
import org.scalatest.{FlatSpec, Matchers}
import testUtils.FutureUtils.await
import testUtils.services.TestIdentityService.futureUser
import testUtils.services.TestSalesforceService.futureFailure
import testUtils.services.{TestIdentityService, TestSalesforceService}

class CheckoutServiceTest extends FlatSpec with Matchers {


  val subscriptionData: SubscriptionData = SubscriptionData(
    PersonalData("", "", "", AddressData("", "", "", "")),
    PaymentData("", 1, 2, 3, ""))

  "Process subscription" should "succeed when valid SC_GU_U cookie is provided" in {
    val subject = new CheckoutService(TestIdentityService(_userLookupByScGuU = futureUser), TestSalesforceService())
    await(subject.processSubscription(subscriptionData, Some("valid SC_GU_U"))) should be(Right("AB123456"))
  }

  "Process subscription" should "fail when invalid SC_GU_U cookie is provided" in {
    val subject = new CheckoutService(TestIdentityService(), TestSalesforceService())
    await(subject.processSubscription(subscriptionData, Some("invalid SC_GU_U"))) should be(Left(InvalidLoginCookie))
  }

  "Process subscription" should "succeed when registred email address is provided" in {
    val subject = new CheckoutService(TestIdentityService(_userLookupByEmail = futureUser), TestSalesforceService())
    await(subject.processSubscription(subscriptionData)) should be(Right("AB123456"))
  }

  "Process subscription" should "succeed when a guest user is sucessfully registered" in {
    val subject = new CheckoutService(TestIdentityService(_registerGuest = futureUser), TestSalesforceService())
    await(subject.processSubscription(subscriptionData)) should be(Right("AB123456"))
  }

  "Process subscription" should "fail when a guest user is not sucessfully registered" in {
    val subject = new CheckoutService(TestIdentityService(), TestSalesforceService())
    await(subject.processSubscription(subscriptionData)).leftSideValue should be(Left(GuestUserNotCreated))
  }

  "Process subscription" should "fail when a Salesforce user cannot be created" in {
    val subject = new CheckoutService(TestIdentityService(), TestSalesforceService(futureFailure))
    await(subject.processSubscription(subscriptionData)).leftSideValue should be(Left(GuestUserNotCreated))
  }

}

