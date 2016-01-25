package acceptance.pages

import acceptance.util.{Browser, TestUser, Config}
import Config.baseUrl
import org.scalatest.selenium.Page

class Checkout(val testUser: TestUser) extends Page with Browser {
  val url = s"$baseUrl/checkout"

  val formErrorClass = ".form-field--error"

  object PersonalDetails {
    lazy val firstName = textField(name("personal.first"))
    lazy val lastName = textField(name("personal.last"))
    lazy val email = emailField(name("personal.emailValidation.email"))
    lazy val emailConfirmation = emailField(name("personal.emailValidation.confirm"))
    lazy val address1 = textField(name("personal.address.address1"))
    lazy val address2 = textField(name("personal.address.address2"))
    lazy val town = textField(name("personal.address.town"))
    lazy val postcode = textField(name("personal.address.postcode"))
    lazy val receiveGnmMarketing = checkbox(name("personal.receiveGnmMarketing"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("address-postcode")))

      val emailValue = s"${testUser.username}@gu.com"
      firstName.value = s"${testUser.username}"
      lastName.value = s"${testUser.username}"
      email.value = emailValue
      emailConfirmation.value = emailValue
      address1.value = "address 1"
      address2.value = "address 2"
      town.value = "town"
      postcode.value = "E8123"
    }

    def fillInAddress(): Unit = {
      assert(pageHasElement(id("address-postcode")))

      address1.value = "address 1"
      address2.value = "address 2"
      town.value = "town"
      postcode.value = "E8123"
    }

    def continue(): Unit = {
      val selector = cssSelector(".js-checkout-your-details-submit")
      assert(pageHasElement(selector))
      click.on(selector)
    }
  }

  object DebitCardPaymentDetails {
    val account = textField(name("payment.account"))
    val sortcode = textField(name("payment.sortcode"))
    val payment = textField(name("payment.holder"))
    val confirm = checkbox(cssSelector(""".js-checkout-confirm-payment input[type="checkbox"]"""))

    def fillIn(): Unit = {
      assert(pageHasElement(id("payment-holder")))

      account.value = "55779911"
      sortcode.value = "200000"
      payment.value = "payment"
      confirm.select()
    }

    def continue(): Unit = {
      val selector = cssSelector(".js-checkout-payment-details-submit")
      assert(pageHasElement(selector))
      click.on(selector)
    }
  }

  object CreditCardPaymentDetails {

    val paymentType = radioButtonGroup("payment.type")

    val cardNumber = textField(id("payment-card-number"))
    val cardExpiryMonth = textField(id("payment-card-expiry-month"))
    val cardExpiryYear = textField(id("payment-card-expiry-year"))
    val cardCvc = textField(id("payment-card-cvc"))

    val continueButton = cssSelector(".js-checkout-payment-details-submit")

    def fillIn(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4242424242424242"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    /* https://stripe.com/docs/testing */

    // Charge will be declined with a card_declined code.
    def fillInCardDeclined(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4000000000000002"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    // Charge will be declined with a card_declined code and a fraudulent reason.
    def fillInCardDeclinedFraud(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4100000000000019"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    // Charge will be declined with an incorrect_cvc code.
    def fillInCardDeclinedCvc(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4000000000000127"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    // Charge will be declined with an expired_card code.
    def fillInCardDeclinedExpired(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4000000000000069"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    // Charge will be declined with a processing_error code.
    def fillInCardDeclinedProcessError(): Unit = {
      assert(pageHasElement(id("payment-card-cvc")))

      cardNumber.value = "4000000000000119"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "19"
      cardCvc.value = "111"
    }

    def continue(): Unit = {
      assert(pageHasElement(continueButton))
      click.on(continueButton)
    }
  }

  def fillInPersonalDetails(): Unit = {
    PersonalDetails.fillIn()
    PersonalDetails.continue()
  }

  def fillInAddressDetails(): Unit = {
    PersonalDetails.fillInAddress()
    PersonalDetails.continue()
  }

  def fillInDirectDebitPaymentDetails(): Unit = {
    DebitCardPaymentDetails.fillIn()
    DebitCardPaymentDetails.continue()
  }

  def fillInCreditCardPaymentDetails(): Unit = {
    CreditCardPaymentDetails.fillIn()
    CreditCardPaymentDetails.continue()
  }

  def fillInCardDeclined(): Unit = {
    CreditCardPaymentDetails.fillInCardDeclined()
    CreditCardPaymentDetails.continue()
  }

  def fillInCardDeclinedFraud(): Unit = {
    CreditCardPaymentDetails.fillInCardDeclinedFraud()
    CreditCardPaymentDetails.continue()
  }

  def fillInCardDeclinedCvc(): Unit = {
    CreditCardPaymentDetails.fillInCardDeclinedCvc()
    CreditCardPaymentDetails.continue()
  }

  def fillInCardDeclinedExpired(): Unit = {
    CreditCardPaymentDetails.fillInCardDeclinedExpired()
    CreditCardPaymentDetails.continue()
  }

  def fillInCardDeclinedProcessError(): Unit = {
    CreditCardPaymentDetails.fillInCardDeclinedProcessError()
    CreditCardPaymentDetails.continue()
  }

  def selectCreditCardPaymentOption() = {
    assert(pageHasElement(name("payment.type")))
    CreditCardPaymentDetails.paymentType.value = "card"
  }

  def submit(): Unit = {
    val selector = cssSelector( """.js-checkout-submit""")
    assert(pageHasElement(selector))
    PersonalDetails.receiveGnmMarketing.select()
    click.on(selector)
  }

  def pageHasLoaded(): Boolean = {
    pageHasElement(cssSelector(".js-checkout-your-details-submit"))
  }

  def userDisplayName: String = {
    val selector = cssSelector(".js-user-displayname")
    assert(pageHasElement(selector))
    selector.element.text
  }
}
