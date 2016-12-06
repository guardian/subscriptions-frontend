package acceptance.pages

import acceptance.util.{TestUser, LoadablePage, Browser, Config}

class Register(val testUser: TestUser) extends LoadablePage with Browser {
  val url = s"${Config.identityFrontendUrl}/register?skipConfirmation=true&returnUrl=${Config.baseUrl}/uk"

  def hasLoaded(): Boolean = pageHasElement(createAccountButton)

  def hasCreated(): Boolean = pageHasElement(subscriptionsUKDigital)
  // just anything we expect on config.baseurl page
  private lazy val subscriptionsUKDigital = xpath("//*[@data-test-id='subscriptions-uk-digital']")

  def fillInPersonalDetails(): Unit = RegisterFields.fillIn()

  def createAccount(): Unit = clickOn(createAccountButton)

  private object RegisterFields {
    val firstName = id("register_field_firstname")
    val lastName = id("register_field_lastname")
    val email = id("register_field_email")
    val password = id("register_field_password")
    val displayName = id("register_field_displayName")

    def fillIn() = {
      clear()
      setValue(firstName, testUser.username)
      setValue(lastName, testUser.username)
      setValue(email, s"${testUser.username}@gu.com")
      setValue(password, testUser.username)
      if (pageHasElement(displayName)) {
        // must be the normal rather than membership signup
        setValue(displayName, testUser.username)
      }
    }

    def clear() = {
      clearValue(firstName)
      clearValue(lastName)
      clearValue(email)
      clearValue(password)
      if (pageHasElement(displayName)) {
        clearValue(displayName)
      }
    }
  }

  private lazy val createAccountButton = id("register_submit")
}
