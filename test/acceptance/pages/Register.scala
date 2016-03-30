package acceptance.pages

import acceptance.util.{TestUser, LoadablePage, Browser, Config}

class Register(val testUser: TestUser) extends LoadablePage with Browser {
  val url = s"${Config.identityFrontendUrl}/register?skipConfirmation=true"

  def hasLoaded(): Boolean = pageHasElement(createAccountButton)

  def fillInPersonalDetails(): Unit = RegisterFields.fillIn()

  def createAccount(): Unit = clickOn(createAccountButton)

  private object RegisterFields {
    val firstName = id("register_field_firstname")
    val lastName = id("register_field_lastname")
    val email = id("register_field_email")
    val username = id("register_field_username")
    val password = id("register_field_password")

    def fillIn() = {
      setValue(firstName, testUser.username)
      setValue(lastName, testUser.username)
      setValue(email, s"${testUser.username}@gu.com")
      setValue(username, testUser.username)
      setValue(password, testUser.username)
    }
  }

  private lazy val createAccountButton = id("register_submit")
}
