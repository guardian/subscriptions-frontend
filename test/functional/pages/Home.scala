package functional.pages

import configuration.Config.appUrl
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{Page, WebBrowser}

object Home extends Page with WebBrowser {
  val url = appUrl

  def selectDigital = ???

  def selectPaperPlusDigital()(implicit d: WebDriver) =
    selectPlan("""*[data-test-id="subscriptions-uk-paper-digital"]""")

  def selectPaper()(implicit d: WebDriver) =
    selectPlan( """*[data-test-id="subscriptions-uk-paper"]""")

  private def selectPlan(selector: String)(implicit d: WebDriver) = {
    go.to(this)
    clickOn(cssSelector(selector))
  }
}
