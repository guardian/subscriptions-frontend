package acceptance.pages

import acceptance.util.{TestUser, LoadablePage, Browser, Config}

class RegisterStepOne(val testUser: TestUser) extends LoadablePage with Browser {
  val url = s"${Config.identityFrontendUrl}/signin?skipConfirmation=true&returnUrl=${Config.baseUrl}/uk"

  def fillInEmail() { EmailField.fillIn() }

  def submit() { clickOn(submitButton) }

  def hasLoaded: Boolean = pageHasElement(submitButton)

  private object EmailField {
    val email = id("tssf-email")

    def fillIn() {
      setValue(email, s"${testUser.username}@gu.com")
    }
  }

  private val submitButton = id("tssf-submit")
}

class RegisterStepTwo(val testUser: TestUser) extends LoadablePage with Browser {
  val url = s"${Config.identityFrontendUrl}/signin?skipConfirmation=true&returnUrl=${Config.baseUrl}/uk"

  def fillInPersonalDetails() { RegisterFields.fillIn() }

  def submit() { clickOn(submitButton) }

  def hasLoaded: Boolean = pageHasElement(submitButton)

  private object RegisterFields {
    val firstName = id("register_field_firstname")
    val lastName = id("register_field_lastname")
    val password = name("password")

    def fillIn() {
      setValue(firstName, testUser.username)
      setValue(lastName, testUser.username)
      setValue(password, testUser.username)
    }
  }

  private val submitButton = id("tssf-create-account")

}
