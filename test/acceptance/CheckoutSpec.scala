package acceptance

import acceptance.pages.{Checkout, ThankYou}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}

class CheckoutSpec extends FeatureSpec
  with WebBrowser with Util with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  implicit lazy val driver: WebDriver = Config.driver

  // Before each test ...
  before { resetDriver() }

  // After all tests execute, close all windows, and exit the driver
  override def afterAll(): Unit = { quit() }

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
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }
    }
  }
}
