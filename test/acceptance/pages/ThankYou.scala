package acceptance.pages

import acceptance.util.{WebBrowserUtil, Config}
import Config.baseUrl
import acceptance.util.Config
import org.scalatest.selenium.{Page, WebBrowser}

class ThankYou extends Page with WebBrowser with WebBrowserUtil {
  override val url = s"$baseUrl/checkout/thank-you"

  object PasswordForm {
    val password = pwdField(id("password"))
  }

  def setPassword(pwd: String): Unit = {
    PasswordForm.password.value = pwd
    click.on(cssSelector(".js-checkout-finish-account-submit"))
  }

  def pageHasLoaded(): Boolean = {
    pageHasElement(name("subscriptionDetails"))
  }

  def userDisplayName: String = {
    val selector = cssSelector(".js-user-displayname")
    assert(pageHasElement(selector))
    selector.element.text
  }

  def hasMyProfileButton = {
    pageHasElement(cssSelector(s"a[href='${Config.profileUrl}/account/edit']"))
  }
}
