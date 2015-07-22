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
      withQACookie {
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
      }
    }

    scenario("Guest user supplies invalid details", Acceptance) {
      val checkout = new Checkout()

      import checkout.PersonalDetails

      withQACookie {
        When("I visit the checkout page")
        go.to(checkout)

        And("I fill in personal details with inconsistent emails")
        PersonalDetails.fillIn()
        PersonalDetails.emailConfirmation.value = "non-matching-email@example.com"

        Thread.sleep(1000)
        PersonalDetails.continue()

        Then("The email field should be marked with an error")
        assert(PersonalDetails.emailNotValid(), "email confirmation should not be valid")
      }
    }

    scenario("ordinary access to a pre-release page", Acceptance) {
      val checkout = new Checkout

      Given("No QA cookie is set")

      When("I visit the checkout page ")
      go.to(checkout)

      Then("I should land on the Google Auth page")
      assertResult("accounts.google.com")(currentHost)
    }
  }
}
