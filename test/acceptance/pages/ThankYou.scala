package acceptance.pages

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{Page, WebBrowser}
import acceptance.Config.appUrl

class ThankYou(implicit driver: WebDriver) extends Page with WebBrowser {
  override val url = s"$appUrl/checkout"

  object PasswordForm {
    val password = pwdField(id("password"))
  }

  def setPassword(pwd: String): Unit = {
    PasswordForm.password.value = pwd
    click.on(cssSelector(".js-checkout-finish-account-submit"))
  }
}
