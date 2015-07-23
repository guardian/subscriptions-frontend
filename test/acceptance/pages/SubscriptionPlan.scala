package acceptance.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser

class SubscriptionPlan(implicit val driver: WebDriver) extends  WebBrowser {
  def selectEverydayPackage() = selectPackage("everyday")
  def selectSixdayPackage() = selectPackage("sixday")
  def selectWeekendPackage() = selectPackage("weekend")

  private def selectPackage(id: String) = {
    clickOn(cssSelector(s"""label[for="$id"]"""))
    clickOn(cssSelector("""*[data-test-id="choose-package-select"]"""))
  }
}
