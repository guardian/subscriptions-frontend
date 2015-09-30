package acceptance

import acceptance.pages.{Checkout, ThankYou}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfter, FeatureSpec, GivenWhenThen}

class CheckoutSpec extends FeatureSpec with WebBrowser with Util with GivenWhenThen with BeforeAndAfter {

  implicit lazy val driver: WebDriver = Config.driver

  before {
    resetDriver()
  }

  feature("Checkout page") {
    scenario("Guest user completes a checkout and sets an account password", Acceptance) {
      val checkout = new pages.Checkout
      When("I visit the checkout page ")
      go.to(checkout)

      And("I fill in personal details")
      checkout.fillInPersonalDetails()

      And("I fill in payment details")
      checkout.fillInPaymentDetails()

      And("I submit the form")
      checkout.submit()

      Then("I should land on the Thank You page")
      assert(pageHasText("Thank you for your order"))

      When("I set a password for my account")
      new ThankYou().setPassword("supers4af3passw0rd")

      Then("I should read a confirmation message")
      assert(pageHasText("All done"))

      And("I should have Identity cookies")
      go.to(checkout)
      Seq("GU_U", "SC_GU_U").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }
    }

    scenario("Guest user supplies invalid details", Acceptance) {
      val checkout = new Checkout()
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
}
