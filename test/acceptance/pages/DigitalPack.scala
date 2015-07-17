package acceptance.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}
import acceptance.Config.appUrl

object DigitalPack extends Page with WebBrowser {
  override val url = s"$appUrl/digital/country"

  def selectUK(implicit d: WebDriver) = {
    selectCountry("uk")
  }

  def selectNonUK(implicit d: WebDriver) = {
    selectCountry("Not the United Kingdom")
  }

  private def selectCountry(value: String)(implicit d: WebDriver):Unit = {
    go.to(this)
    singleSel("country").value = value
    click on cssSelector("""button[type="submit"]""")
  }
}
