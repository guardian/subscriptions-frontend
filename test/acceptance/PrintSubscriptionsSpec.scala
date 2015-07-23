package acceptance

import acceptance.Config.appUrl
import acceptance.pages.{Home, SubscriptionPlan}
import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatest.selenium.WebBrowser

case class SubscriptionTest(url: String, name: String, landingHost: String)

object SubscriptionTest {
  def collection(url: String, name: String) =
    SubscriptionTest(url, name, "www.guardiansubscriptions.co.uk")
  def delivery(url: String, name: String) =
    SubscriptionTest(url, name, "www.guardiandirectsubs.co.uk")
}

class PrintSubscriptionsSpec extends FeatureSpec with Util with WebBrowser with GivenWhenThen with BeforeAndAfter {
  implicit lazy val driver: WebDriver = Config.driver

  before {
    resetDriver()
  }

  val testData = Seq(
    SubscriptionTest.collection("/collection/paper", "Paper voucher subscription"),
    SubscriptionTest.collection("/collection/paper-digital", "Paper + digital voucher subscription"),
    SubscriptionTest.delivery("/delivery/paper", "Paper home delivery subscription"),
    SubscriptionTest.delivery("/delivery/paper-digital", "Paper + digital home delivery subscription")
  )

  feature("Print subscriptions") {
    for (test <- testData) yield scenario(test.name, Acceptance) {
      go.to(s"$appUrl/${test.url}")
      new SubscriptionPlan().selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult(test.landingHost)(currentHost)
    }

    scenario("Paper + Digital subscription", Acceptance) {
      Home.selectPaperPlusDigital()
      new SubscriptionPlan().selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }

    scenario("Paper only subscription", Acceptance) {
      Home.selectPaper()
      new SubscriptionPlan().selectWeekendPackage()
      assert(pageHasText("You have chosen\nWEEKEND"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }
  }
}
