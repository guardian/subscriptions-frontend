package acceptance

import acceptance.Config.appUrl
import acceptance.pages.{DigitalPack, Home, SubscriptionPlan}
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

class PrintSubscriptionsSpec extends FreeSpec with Util with WebBrowser {
  implicit lazy val driver: WebDriver = Config.driver

  val testData = Seq(
    SubscriptionTest.collection("/collection/paper", "Paper voucher subscription"),
    SubscriptionTest.collection("/collection/paper-digital", "Paper + digital voucher subscription"),
    SubscriptionTest.delivery("/delivery/paper", "Paper home delivery subscription"),
    SubscriptionTest.delivery("/delivery/paper-digital", "Paper + digital home delivery subscription")
  )

  "Lucrative subscriptions" - {
    for (test <- testData) yield test.name.taggedAs(Acceptance) in {
      goTo(s"$appUrl/${test.url}")
      SubscriptionPlan.selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult(test.landingHost)(currentHost)
    }

    "Paper + Digital subscription" taggedAs Acceptance in {
      Home.selectPaperPlusDigital()
      SubscriptionPlan.selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }

    "Paper only subscription" taggedAs Acceptance in {
      Home.selectPaper()
      SubscriptionPlan.selectWeekendPackage()
      assert(pageHasText("You have chosen\nWEEKEND"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }
  }

  "Digital Pack" - {
    "selecting UK" taggedAs Acceptance in {
      pending
      DigitalPack.selectUK
      assert(pageHasText("You have chosen:\nDigital pack"))
    }

    "selecting not UK" taggedAs Acceptance in {
      pending
      DigitalPack.selectNonUK
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }
  }
}
