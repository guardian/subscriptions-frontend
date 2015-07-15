package acceptance

import acceptance.pages.Checkout
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import org.scalatest.{GivenWhenThen, BeforeAndAfterAll, FeatureSpec}

class CheckoutSpec extends FeatureSpec with WebBrowser with Util with GivenWhenThen{
  implicit lazy val driver: WebDriver = Config.driver

  feature("pre-release pages") {
    scenario("QA access to a pre-release page") {
      withQACookie {
        Given("The QA cookie is set")

        When("I visit the checkout page ")
        go.to(Checkout)

        Then("I should see the digital pack checkout page")
        assert(pageHasText("Digital Pack"))
      }
    }

    scenario("ordinary access to a pre-release page") {
      Given("No QA cookie is set")
      When("I visit the checkout page ")
      go.to(Checkout)

      Then("I should land on the Google Auth page")
      assertResult("accounts.google.com")(currentHost)
    }
  }
}
