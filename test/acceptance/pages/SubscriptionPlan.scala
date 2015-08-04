package acceptance.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser

object SubscriptionPlan extends WebBrowser {
  def selectEverydayPackage()(implicit d: WebDriver) = selectPackage("everyday")
  def selectSixdayPackage()(implicit d: WebDriver) = selectPackage("sixday")
  def selectWeekendPackage()(implicit d: WebDriver) = selectPackage("weekend")

  private def selectPackage(id: String)(implicit d: WebDriver) = {
    clickOn(cssSelector(s"""*[data-test-id="subscription-package-$id"]"""))
  }
}
