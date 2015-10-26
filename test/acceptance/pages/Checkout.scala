package acceptance.pages


import acceptance.Config.appUrl
import acceptance.{Config, Util}
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}



class Checkout(implicit val driver: WebDriver) extends Page with WebBrowser with Util {
  val url = s"$appUrl/checkout"

  val formErrorClass = ".form-field--error"

  object PersonalDetails {
    val firstName = textField(name("personal.first"))
    val lastName = textField(name("personal.last"))
    val email = emailField(name("personal.emailValidation.email"))
    val emailConfirmation = emailField(name("personal.emailValidation.confirm"))
    val address1 = textField(name("personal.address.address1"))
    val address2 = textField(name("personal.address.address2"))
    val town = textField(name("personal.address.town"))
    val postcode = textField(name("personal.address.postcode"))
    val receiveGnmMarketing = checkbox(name("personal.receiveGnmMarketing"))

    def fillIn(): Unit = {

      import com.gu.identity.testing.usernames.TestUsernames
      import com.github.nscala_time.time.Imports._

      val testUsers = TestUsernames(
        com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
        recency = 2.days.standardDuration
      )
      val testUserString = testUsers.generate()

      val emailValue = s"${testUserString}@gu.com"
      firstName.value = s"${testUserString}"
      lastName.value = s"${testUserString}"
      email.value = emailValue
      emailConfirmation.value = emailValue
      address1.value = "address 1"
      address2.value = "address 2"
      town.value = "town"
      postcode.value = "E8123"
    }

    def emailNotValid(): Boolean =
      fieldHasError(emailConfirmation)

    def continue(): Unit = {
      click.on(cssSelector(".js-checkout-your-details-submit"))
    }
  }

  object PaymentDetails {
    val account = textField(name("payment.account"))
    val sortcode = textField(name("payment.sortcode"))
    val payment = textField(name("payment.holder"))
    val confirm = checkbox(cssSelector(""".js-checkout-confirm-payment input[type="checkbox"]"""))

    def fillIn(): Unit = {
      account.value = "55779911"
      sortcode.value = "200000"
      payment.value = "payment"
      confirm.select()
    }

    def continue(): Unit = {
      val selector = cssSelector(".js-checkout-payment-details-submit")
      pageHasElement(selector)
      click.on(selector)
    }
  }

  private def fieldHasError(elem: Element): Boolean = {
    elem.attribute("name").map { inputName =>
      pageHasElement(cssSelector(s"""$formErrorClass *[name="$inputName"]"""), 5)
    }.isDefined
  }

  def fillInPersonalDetails(): Unit = {
    PersonalDetails.fillIn()
    PersonalDetails.continue()
  }

  def fillInPersonalDetailsTestUser(): Unit = {
    PersonalDetails.fillIn()
    PersonalDetails.continue()
  }

  def fillInPaymentDetails(): Unit = {
    PaymentDetails.fillIn()
    PaymentDetails.continue()
  }

  def submit(): Unit = {
    val selector = cssSelector( """input[type="submit"]""")
    assert(pageHasElement(selector))
    PersonalDetails.receiveGnmMarketing.select()
    click.on(selector)
  }
}
