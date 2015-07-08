package functional

import java.net.URL

import configuration.Config.appUrl
import functional.pages.{Home, SubscriptionPlan}
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

class PrintSubscriptionsSpec extends FreeSpec with ShouldMatchers with WebBrowser with BeforeAndAfterAll {
  implicit lazy val driver: WebDriver = Driver.fromConfig

  val testData = Seq(
    SubscriptionTest.collection(viewCollectionPaper.toString, "Paper voucher subscription"),
    SubscriptionTest.collection(viewCollectionPaperDigital.toString, "Paper + digital voucher subscription"),
    SubscriptionTest.delivery(viewDeliveryPaper.toString, "Paper home delivery subscription"),
    SubscriptionTest.delivery(viewDeliveryPaperDigital.toString, "Paper + digital home delivery subscription")
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

  private def currentHost: String = new URL(currentUrl).getHost
  private def pageHasText(text: String): Boolean = {
    find(tagName("body")).get.text.contains(text)
  }

  override def afterAll() {
    quit()
  }
}
