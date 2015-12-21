package acceptance

import acceptance.pages.{Checkout, ThankYou}
import acceptance.util._
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class CheckoutSpec extends FeatureSpec with WebBrowser with WebBrowserUtil
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { // Before each test
    resetDriver()
  }

  override def beforeAll() = {
    Screencast.storeId()
    Config.printSummary()
  }

  // After all tests execute, close all windows, and exit the driver
  override def afterAll() = {
    Config.driver.quit()
  }

  feature("Guest user subscription checkout") {
    scenario("Guest user subscribes with direct debit", Acceptance) {
      val checkout = new Checkout(new TestUser)
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
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie))
      }
    }
  }
}
