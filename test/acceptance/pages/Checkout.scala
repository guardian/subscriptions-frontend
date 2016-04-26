package acceptance.pages

import acceptance.util.{Browser, TestUser, Config}
import Config.baseUrl
import org.scalatest.selenium.Page

case class Checkout(val testUser: TestUser) extends Page with Browser {
  val url = s"$baseUrl/checkout"

  def fillInPersonalDetails(): Unit = PersonalDetails.fillIn()

  def clickPersonalDetailsContinueButton() = PersonalDetails.continue()

  def fillInAddressDetails(): Unit = PersonalDetails.fillInAddress()

  def fillInDirectDebitPaymentDetails(): Unit = DebitCardPaymentDetails.fillIn()

  def selectConfirmAccountHolder(): Unit = DebitCardPaymentDetails.confirm.select()

  def clickDebitPaymentContinueButton(): Unit = DebitCardPaymentDetails.continue()

  def fillInCreditCardPaymentDetails(): Unit = CreditCardPaymentDetails.fillIn()

  def clickCredictCardPaymentContinueButton() = CreditCardPaymentDetails.continue()

  def selectCreditCardPaymentOption() = setRadioButtonValue(CreditCardPaymentDetails.paymentType, "card")

  def submitPayment(): Unit = clickOn(submitPaymentButton)

  def pageHasLoaded(): Boolean = pageHasElement(cssSelector(".js-checkout-your-details-submit"))

  def yourDetailsSectionHasLoaded(): Boolean = pageHasElement(PersonalDetails.continueButton)

  def directDebitSectionHasLoaded(): Boolean = pageHasElement(DebitCardPaymentDetails.continueButton)

  def cardSectionHasLoaded(): Boolean = pageHasElement(CreditCardPaymentDetails.continueButton)

  def reviewSectionHasLoaded(): Boolean = pageHasElement(ReviewSection.submitPaymentButton)

  def userIsSignedIn: Boolean = elementHasText(userDisplayName, testUser.username.toLowerCase)

  def userDetailsArePrefilled: Boolean = {
    elementHasValue(PersonalDetails.firstName, testUser.username) &&
      elementHasValue(PersonalDetails.lastName, testUser.username) &&
      elementHasValue(PersonalDetails.email, s"${testUser.username.toLowerCase}@gu.com")
  }

  private val userDisplayName = cssSelector(".js-user-displayname")
  private val submitPaymentButton = cssSelector(".js-checkout-submit")

  private object
  PersonalDetails {
    val firstName = id("first")
    val lastName = id("last")
    val email = id("email")
    val emailConfirm = id("confirm")
    val address1 = id("address-line-1")
    val address2 = id("address-line-2")
    val town = id("address-town")
    val postcode = id("address-postcode")
    val continueButton = cssSelector(".js-checkout-your-details-submit")

    def fillIn(): Unit = {
      setValue(firstName, s"${testUser.username}")
      setValue(lastName, s"${testUser.username}")
      setValue(email, s"${testUser.username}@gu.com")
      setValue(emailConfirm, s"${testUser.username}@gu.com")
      setValue(address1, "address 1")
      setValue(address2, "address 2")
      setValue(town, "town")
      setValue(postcode, "E8123")
    }

    def fillInAddress(): Unit = {
      setValue(address1, "address 1")
      setValue(address2, "address 2")
      setValue(town, "town")
      setValue(postcode, "E8123")
    }

    def continue(): Unit = clickOn(continueButton)
  }

  private object DebitCardPaymentDetails {
    val account = id("payment-account")
    val sortcode = id("payment-sortcode")
    val payment = id("payment-holder")
    val confirm = checkbox(cssSelector(""".js-checkout-confirm-payment input[type="checkbox"]"""))
    val continueButton = cssSelector(".js-checkout-payment-details-submit")

    def fillIn(): Unit = {
      setValue(account, "55779911")
      setValue(sortcode, "200000")
      setValue(payment, "payment")
    }

    def continue(): Unit = clickOn(continueButton)
  }

  private object CreditCardPaymentDetails {
    val paymentType = name("payment.type")
    val cardNumber = id("payment-card-number")
    val cardExpiryMonth = id("payment-card-expiry-month")
    val cardExpiryYear = id("payment-card-expiry-year")
    val cardCvc = id("payment-card-cvc")
    val continueButton = cssSelector(".js-checkout-payment-details-submit")

    private def fillInHelper(cardNum: String) = {
      setValue(cardNumber, "4242424242424242")
      setValue(cardExpiryMonth, "10")
      setValue(cardExpiryYear, "19")
      setValue(cardCvc, "111")
    }

    def fillIn(): Unit = fillInHelper("4242424242424242")

    /* https://stripe.com/docs/testing */

    // Charge will be declined with a card_declined code.
    def fillInCardDeclined(): Unit = fillInHelper("4000000000000002")

    // Charge will be declined with a card_declined code and a fraudulent reason.
    def fillInCardDeclinedFraud(): Unit = fillInHelper("4100000000000019")

    // Charge will be declined with an incorrect_cvc code.
    def fillInCardDeclinedCvc(): Unit = fillInHelper("4000000000000127")

    // Charge will be declined with an expired_card code.
    def fillInCardDeclinedExpired(): Unit = fillInHelper("4000000000000069")

    // Charge will be declined with a processing_error code.
    def fillInCardDeclinedProcessError(): Unit = fillInHelper("4000000000000119")

    def continue(): Unit = clickOn(continueButton)
  }

  private object ReviewSection {
    val submitPaymentButton = cssSelector(".js-checkout-submit")
  }
}
