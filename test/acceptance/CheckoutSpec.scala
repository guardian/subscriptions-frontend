package acceptance

import acceptance.pages.{Checkout, ThankYou}
import acceptance.util.{Util, TestUser, Config, Acceptance}
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class CheckoutSpec extends FeatureSpec with WebBrowser with Util
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before {
    resetDriver()
  } // Before each test

  override def beforeAll() = {
    Config.printSummary()
  }

  // After all tests execute, close all windows, and exit the driver
  override def afterAll() = {
    Config.driver.quit()
  }

  feature("Guest user subscription checkout") {
    scenario("Guest user subscribes with direct debit", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("I fill in direct debit payment details")
      checkout.fillInDirectDebitPaymentDetails()

      And("I submit the form")
      checkout.submit()

      Then("I should land on the Thank You page")
      assert(pageHasElement(name("subscriptionDetails")))

      When("I set a password for my account")
      val thankYou = new ThankYou
      thankYou.setPassword("supers4af3passw0rd")

      Then("I should see 'My Profile' button")
      assert(thankYou.hasMyProfileButton)

      And("I should have Identity cookies")
      go.to(checkout)
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }
    }

    scenario("Guest user subscribes with a credit card", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details")
      checkout.fillInCreditCardPaymentDetails()

      And("I submit the form")
      checkout.submit()

      Then("I should land on the Thank You page")
      val thankYou = new ThankYou()
      thankYou.pageHasLoaded()
      assert(pageHasElement(name("subscriptionDetails")))

      When("I set a password for my account")
      thankYou.setPassword("supers4af3passw0rd")

      Then("I should see 'My Profile' button")
      assert(thankYou.hasMyProfileButton)

      And("I should have Identity cookies")
      go.to(checkout)
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }
    }

    scenario("Guest user supplies invalid details", Acceptance) {
      val checkout = new Checkout(new TestUser)
      import checkout.PersonalDetails

      When("I visit the checkout page")
      go.to(checkout)

      And("I fill in personal details with inconsistent emails")
      PersonalDetails.fillIn()
      PersonalDetails.emailConfirmation.value = "non-matching-email@example.com"

      PersonalDetails.continue()

      Then("The email field should be marked with an error")
      assert(PersonalDetails.emailNotValid(), "email confirmation should not be valid")
    }
  }

  feature("Singed-in user subscription checkout") {
    scenario("Signed in user subscribes with a direct debit", Acceptance) {
      val testUser = new TestUser

      Given("I clicked on Subscriptions homepage " +
        "'Subscribe now' --> 'Start your free trial' --> 'United Kingdom'")

      When("I land on 'Identity Register' page")
      val register = new pages.Register(new TestUser)
      go.to(register)
      assert(register.pageHasLoaded())

      And("I fill in personal details")
      register.fillInPersonalDetails()

      And("I submit the form to create my Identity account")
      register.submit()

      Then("I should land back on Checkout page")
      val checkout = new pages.Checkout(testUser)
      assert(checkout.pageHasLoaded())

      And("I should be signed in with my Identity account")
      assert(checkout.userDisplayName == testUser.username.toLowerCase)

      And("first name, last name, and email address should be pre-filled")
      assert(checkout.PersonalDetails.firstName.value == testUser.username)
      assert(checkout.PersonalDetails.lastName.value == testUser.username)
      assert(checkout.PersonalDetails.email.value == s"${testUser.username.toLowerCase}@gu.com")

      And("I should have Identity cookies.")
      go.to(checkout)
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }

      When("I fill in the address")
      checkout.fillInAddressDetails()

      And("I fill in direct debit payment details")
      checkout.fillInDirectDebitPaymentDetails()

      And("I submit the form")
      checkout.submit()

      Then("I should land on the Thank You page")
      val thankYou = new ThankYou()
      thankYou.pageHasLoaded()
      assert(pageHasElement(name("subscriptionDetails")))

      And("I should still be signed in with my Identity account.")
      assert(thankYou.userDisplayName == testUser.username.toLowerCase)
    }

    scenario("Signed in user subscribes with a credit card", Acceptance) {

      val testUser = new TestUser

      Given("I clicked on Subscriptions homepage " +
        "'Subscribe now' --> 'Start your free trial' --> 'United Kingdom'")

      When("I land on 'Identity Register' page")
      val register = new pages.Register(testUser)
      go.to(register)
      assert(register.pageHasLoaded())

      And("I fill in personal details")
      register.fillInPersonalDetails()

      And("I submit the form to create my Identity account")
      register.submit()

      Then("I should land back on Checkout page")
      val checkout = new pages.Checkout(testUser)
      assert(checkout.pageHasLoaded())

      And("I should be signed in with my Identity account")
      assert(checkout.userDisplayName == testUser.username.toLowerCase)

      And("first name, last name, and email address should be pre-filled")
      assert(checkout.PersonalDetails.firstName.value == testUser.username)
      assert(checkout.PersonalDetails.lastName.value == testUser.username)
      assert(checkout.PersonalDetails.email.value == s"${testUser.username.toLowerCase}@gu.com")

      And("I should have Identity cookies.")
      go.to(checkout)
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }

      When("I fill in the address")
      checkout.fillInAddressDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details")
      checkout.fillInCreditCardPaymentDetails()

      And("I submit the form")
      checkout.submit()

      Then("I should land on the Thank You page")
      val thankYou = new ThankYou()
      thankYou.pageHasLoaded()
      assert(pageHasElement(name("subscriptionDetails")))

      And("I should still be signed in with my Identity account.")
      assert(thankYou.userDisplayName == testUser.username.toLowerCase)
    }
  }

  feature("Checkout credit card failures") {
    scenario("Charge is declined with a card_declined code", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details (card_declined)")
      checkout.fillInCardDeclined()

      And("I submit the form")
      checkout.submit()

      Then("the credit card should be declined")
      assert(pageHasElement(id("payment-card-number")))

      And("I remain on Checkout page")
      assert(currentUrl(Config.driver) == checkout.url)
    }

    scenario("Charge will be declined with an incorrect_cvc code.", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details (incorrect_cvc)")
      checkout.fillInCardDeclinedCvc()

      And("I submit the form")
      checkout.submit()

      Then("the credit card should be declined")
      assert(pageHasElement(id("payment-card-number")))

      And("I remain on Checkout page")
      assert(currentUrl(Config.driver) == checkout.url)
    }

    scenario("Charge will be declined with an expired_card code.", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details (expired_card)")
      checkout.fillInCardDeclinedExpired()

      And("I submit the form")
      checkout.submit()

      Then("the credit card should be declined")
      assert(pageHasElement(id("payment-card-number")))

      And("I remain on Checkout page")
      assert(currentUrl(Config.driver) == checkout.url)
    }

    scenario("Charge will be declined with a processing_error code.", Acceptance) {
      val checkout = new pages.Checkout(new TestUser)
      When("I visit the checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("Select credit card payment option")
      checkout.selectCreditCardPaymentOption()

      And("I fill in credit card payment details (processing_error)")
      checkout.fillInCardDeclinedProcessError()

      And("I submit the form")
      checkout.submit()

      Then("the credit card should be declined")
      assert(pageHasElement(id("payment-card-number")))

      And("I remain on Checkout page")
      assert(currentUrl(Config.driver) == checkout.url)
    }
  }
}
