package acceptance.pages


import acceptance.Config.appUrl
import acceptance.Util
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
      val emailValue = s"test-${System.currentTimeMillis()}@gu.com"
      firstName.value = "first"
      lastName.value = "last"
      email.value = emailValue
      emailConfirmation.value = emailValue
      address1.value = "address 1"
      address2.value = "address 2"
      town.value = "town"
      postcode.value = "E8123"
      receiveGnmMarketing.select()
    }

    def emailNotValid(): Boolean =
      fieldHasError(emailConfirmation)

    def continue(): Unit = {
      click.on(cssSelector(".js-checkout-your-details-submit"))
    }
  }

  object PaymentDetails {
    val account = numberField(name("payment.account"))
    val sortcode1 = numberField(name("payment.sortcode1"))
    val sortcode2 = numberField(name("payment.sortcode2"))
    val sortcode3 = numberField(name("payment.sortcode3"))
    val payment = textField(name("payment.holder"))
    val confirm = checkbox(cssSelector(""".js-checkout-confirm-payment input[type="checkbox"]"""))

    def fillIn(): Unit = {
      account.value = "55779911"
      sortcode1.value = "20"
      sortcode2.value = "00"
      sortcode3.value = "00"
      payment.value = "payment"
      confirm.select()
    }

    def continue(): Unit = {
      click.on(cssSelector(".js-checkout-payment-details-submit"))
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

  def fillInPaymentDetails(): Unit = {
    PaymentDetails.fillIn()
    PaymentDetails.continue()
  }

  def submit(): Unit = {
    click.on(cssSelector("""input[type="submit"]"""))
  }
}
