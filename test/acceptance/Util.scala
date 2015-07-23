package acceptance

import java.net.URL
import java.util.concurrent.TimeUnit

import org.openqa.selenium.support.ui.{WebDriverWait, ExpectedConditions}
import org.openqa.selenium.{By, Cookie, WebDriver}
import org.scalatest.selenium.WebBrowser
import acceptance.Config.appUrl
import configuration.QA.{passthroughCookie => qaCookie}

trait Util { this: WebBrowser =>
  implicit val driver: WebDriver

  protected def withQACookie(block: => Unit): Unit = {
    val cookie = new Cookie(qaCookie.name, qaCookie.value)
    go.to(appUrl)
    driver.manage().addCookie(cookie)

    try block finally {
      driver.manage().deleteCookie(cookie)
    }
  }

  def resetDriver() = {
    driver.get("about:about")
    driver.manage().deleteAllCookies()
    driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS)
  }

  protected def currentHost: String = new URL(currentUrl).getHost

  protected def pageHasText(text: String, timeoutSecs: Int=50): Boolean = {
    val pred = ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text)
    new WebDriverWait(driver, timeoutSecs).until(pred)
  }
}
