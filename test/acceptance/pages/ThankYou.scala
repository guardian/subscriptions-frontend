package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config.baseUrl
import org.scalatestplus.selenium.Page

case class ThankYou(val testUser: TestUser) extends Page with Browser {
  override val url = s"$baseUrl/checkout/thank-you"

  object PasswordForm {
    val password = pwdField(id("password"))
  }

  def setPassword(pwd: String): Unit = {
    PasswordForm.password.value = pwd
    click.on(cssSelector(".js-checkout-finish-account-submit"))
  }

  def pageHasLoaded(): Boolean = elementIsPresent(subscriptionDetails)

  def userIsSignedIn: Boolean = elementHasText(userDisplayName, testUser.username)

  def hasMyProfileButton = pageHasElement(myProfileButton)

  private val userDisplayName =  cssSelector(".js-user-displayname")
  private val subscriptionDetails = name("subscriptionDetails")
  private val myProfileButton = cssSelector(s"a[href='${Config.identityFrontendUrl}/account/edit']")
}
