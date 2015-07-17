package acceptance

import java.net.URL

import org.openqa.selenium.{Cookie, WebDriver}
import org.scalatest.selenium.WebBrowser
import acceptance.Config.appUrl
import configuration.QA.{passthroughCookie => qaCookie}

trait Util { this: WebBrowser =>

  protected def withQACookie(block: => Unit)(implicit driver: WebDriver): Unit = {
    val cookie = new Cookie(qaCookie.name, qaCookie.value)
    go.to(appUrl)
    driver.manage().addCookie(cookie)

    try block finally {
      driver.manage().deleteCookie(cookie)
    }
  }

  protected def currentHost(implicit d: WebDriver): String = new URL(currentUrl).getHost
  protected def pageHasText(text: String)(implicit d: WebDriver): Boolean = {
    find(tagName("body")).get.text.contains(text)
  }

}
