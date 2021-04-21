package acceptance.pages

import acceptance.util.{Config, TestUser, Browser}
import Config.identityFrontendUrl
import org.scalatest.selenium.Page

class Signin(testUser: TestUser) extends Page with Browser {
  val url = s"${identityFrontendUrl}/signin"

  def signUp() = clickOn(signUpLink)

  def signIn() = clickOn(signInButton)

  def signInWithFacebook() = clickOn(signInWithFacebookButton)

  def signInWithGoogle() = clickOn(signInWithGoogleButton)

  def pageHasLoaded(): Boolean = pageHasElement(signUpLink)

  def fillInCredentials() = SigninFields.fillIn()

  private object SigninFields {
    val emailAddress = id("signin_field_email")
    val password = id("signin_field_password")

    def fillIn() = {
      setValue(emailAddress, s"${testUser.username}@gu.com")
      setValue(password, testUser.username)
    }
  }

  private lazy val signUpLink = id("register_cta")
  private lazy val signInButton = id("signin_submit")
  private lazy val signInWithFacebookButton = id("oauth_cta_facebook")
  private lazy val signInWithGoogleButton = id("oauth_cta_google")
}
