package functional

import functional.pages.{Home, SubscriptionPlan}
import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatest.selenium.WebBrowser

class PrintSubscriptionsSpec extends FreeSpec with ShouldMatchers with WebBrowser with BeforeAndAfterAll {
  implicit val driver: WebDriver = Driver.fromConfig

  "selecting a subscription managed through guardiansubscriptions.co.uk" - {
    "Paper + Digital subscription" taggedAs(Core, Acceptance) in {
      Home.selectPaperPlusDigital()
      SubscriptionPlan.selectSixdayPackage()
      assert(SubscriptionPlan.pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
    }

    "Paper only subscription" taggedAs(Core, Acceptance) in {
      Home.selectPaper()
      SubscriptionPlan.selectWeekendPackage()
      assert(SubscriptionPlan.pageHasText("You have chosen\nWEEKEND"), "document contains selected plan")
    }
  }

  override def afterAll() {
    quit()
  }
}
