package functional

import java.net.URL

import controllers.routes.Shipping.{viewCollectionPaper, viewCollectionPaperDigital, viewDeliveryPaperDigital, viewDeliveryPaper}
import configuration.Config.appUrl
import functional.pages.{Home, SubscriptionPlan}
import org.openqa.selenium.WebDriver
import org.scalatest._
import org.scalatest.selenium.WebBrowser

case class Subscription(name: String, landingHost: String)

class PrintSubscriptionsSpec extends FreeSpec with ShouldMatchers with WebBrowser with BeforeAndAfterAll {
  implicit lazy val driver: WebDriver = Driver.fromConfig

  "selecting a subscription managed through " - {
    "Paper + Digital subscription" taggedAs Acceptance in {
      Home.selectPaperPlusDigital()
      SubscriptionPlan.selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }

    "Paper voucher subscription" taggedAs Acceptance in {
      goTo(s"$appUrl/${viewCollectionPaper.toString}")
      SubscriptionPlan.selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult("www.guardiansubscriptions.co.uk")(currentHost)
    }

    "Paper voucher + digital subscription" taggedAs Acceptance in {
      goTo(s"$appUrl/${viewCollectionPaperDigital.toString}")
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

    "Paper home delivery + digital subscription" taggedAs Acceptance in {
      goTo(s"$appUrl/${viewDeliveryPaperDigital.toString}")
      SubscriptionPlan.selectWeekendPackage()
      assert(pageHasText("You have chosen\nWEEKEND"), "document contains selected plan")
      assertResult("www.guardiandirectsubs.co.uk")(currentHost)
    }

    "Paper home delivery subscription" taggedAs Acceptance in {
      goTo(s"$appUrl/${viewDeliveryPaper.toString}")
      SubscriptionPlan.selectSixdayPackage()
      assert(pageHasText("You have chosen\nSIXDAY"), "document contains selected plan")
      assertResult("www.guardiandirectsubs.co.uk")(currentHost)
    }
  }

  private def currentHost: String = new URL(currentUrl).getHost
  private def pageHasText(text: String): Boolean = {
    find(tagName("body")).get.text.contains(text)
  }

  override def afterAll() {
    quit()
  }
}
