package acceptance

import org.openqa.selenium.WebDriver
import org.scalatest.{GivenWhenThen, FeatureSpec}
import org.scalatest.selenium.WebBrowser
import acceptance.Config.appUrl

class TestUsersSpec extends FeatureSpec with WebBrowser with Util with GivenWhenThen {

  implicit lazy val driver: WebDriver = Config.driver

  feature("Test users") {
    scenario("Accessing the test user page with a QA cookie", Acceptance) {
      withQACookie {
        Given("The QA cookie is set")

        When("I visit the test user page")
        goTo(s"$appUrl/test-users")


        Then("I should land on the test users page")
        assert(pageHasText("Test Users"))

        And("I should receive a test user cookie")
        val cookie = driver.manage().getCookieNamed("subscriptions-test-user-name")
        assert(Option(cookie).isDefined, "Cookie is set")
      }
    }

    scenario("Accessing the test user page without a QA cookie") {
      Given("The QA cookie is not set")

      When("I visit the test user page")
      goTo(s"$appUrl/test-users")

      Then("I should land on the google auth page")
      assertResult("accounts.google.com")(currentHost)
    }
  }
}
