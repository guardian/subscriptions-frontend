package acceptance.pages

import acceptance.util.{Browser, Config, Driver, TestUser}
import Config.baseUrl
import org.openqa.selenium.By
import org.scalatest.selenium.Page

case class Checkout(testUser: TestUser, endpoint: String = "checkout") extends Page with Browser {
  val url = s"$baseUrl/$endpoint"

  def basketPreviewProductPaymentHasLoaded(): Boolean = pageHasElement(BasketPreviewProductPayment.priceSummary)

  def currencyOverrideHasLoaded(): Boolean = pageHasElement(BasketPreviewProductPayment.currencyOverride)

  def basketPreviewProductPaymentContains(expected: String) = {
    elementHasText(BasketPreviewProductPayment.priceSummary, expected)
  }

  def fillInPersonalDetails(): Unit = PersonalDetails.fillIn()

  def clickPersonalDetailsContinueButton() = PersonalDetails.continue()

  def deliveryAddressSectionHasLoaded(): Boolean = pageHasElement(DeliveryAddress.continueButton)

  def fillInDeliveryAddressDetails(): Unit = DeliveryAddress.fillIn()

  def clickDeliveryAddressDetailsContinueButton() = DeliveryAddress.continue()

  def clickBillingDetailsContinueButton() = BillingAddress.continue()

  def fillInBillingAddress(): Unit = BillingAddress.fillIn()

  def fillInDirectDebitPaymentDetails(): Unit = DirectDebitPaymentDetails.fillIn()

  def selectConfirmAccountHolder(): Unit = DirectDebitPaymentDetails.confirm.select()

  def clickDebitPaymentContinueButton(): Unit = DirectDebitPaymentDetails.continue()

  def selectCreditCardPaymentOption() = setRadioButtonValue(CreditCardPaymentDetails.paymentType, "card")

  def submitPayment(): Unit = clickOn(submitPaymentButton)

  def pageHasLoaded(): Boolean = pageHasElement(cssSelector(".js-checkout-your-details-submit"))

  def yourDetailsSectionHasLoaded(): Boolean = pageHasElement(PersonalDetails.continueButton)

  def directDebitSectionHasLoaded(): Boolean = pageHasElement(DirectDebitPaymentDetails.continueButton)

  def billingAddressSectionHasLoaded(): Boolean = pageHasElement(BillingAddress.continueButton)

  def reviewSectionHasLoaded(): Boolean = pageHasElement(ReviewSection.submitPaymentButton)

  def userIsSignedIn: Boolean = {elementHasText(userDisplayName, testUser.username)}

  def userDetailsArePrefilled: Boolean = {
    elementHasValue(PersonalDetails.firstName, testUser.username) &&
      elementHasValue(PersonalDetails.lastName, testUser.username) &&
      elementHasValue(PersonalDetails.email, s"${testUser.username.toLowerCase}@gu.com")
  }

  private val userDisplayName = cssSelector(".js-user-displayname")
  private val submitPaymentButton = cssSelector(".js-checkout-submit")

  private object PersonalDetails {
    val firstName = id("first")
    val lastName = id("last")
    val email = id("email")
    val emailConfirm = id("confirm")
    val continueButton = cssSelector(".js-checkout-your-details-submit")

    def fillIn(): Unit = {
      setValue(firstName, s"${testUser.username}")
      setValue(lastName, s"${testUser.username}")
      setValue(email, s"${testUser.username}@gu.com")
      setValue(emailConfirm, s"${testUser.username}@gu.com")
    }

    def continue(): Unit = clickOn(continueButton)
  }

  private object DeliveryAddress {
    val address1 = id("delivery-address1")
    val address2 = id("delivery-address2")
    val town = id("delivery-town")
    val postcode = id("delivery-postcode")
    val deliveryInstructions = name("deliveryInstructions")
    val continueButton = cssSelector(".js-checkout-delivery-details-submit")

    def fillIn(): Unit = {
      setValue(address1, "90 York Way")
      setValue(address2, "King's Cross")
      setValue(town, "London")
      setValue(postcode, "N1 9GU")
    }

    def continue(): Unit = clickOn(continueButton)
  }

  private object BillingAddress {
    val address1 = id("personal-address-address1")
    val address2 = id("personal-address-address2")
    val town = id("personal-address-town")
    val postcode = id("personal-address-postcode")
    val continueButton = cssSelector(".js-checkout-billing-address-submit")

    def fillIn(): Unit = {
      setValue(address1, "address 1")
      setValue(address2, "address 2")
      setValue(town, "town")
      setValue(postcode, "E8123")
    }

    def continue(): Unit = clickOn(continueButton)
  }

  private object DirectDebitPaymentDetails {
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
    val continueButton = cssSelector(".js-checkout-payment-details-submit")

    def continue(): Unit = clickOn(continueButton)
  }

  private object ReviewSection {
    val submitPaymentButton = cssSelector(".js-checkout-submit")
  }

  private object BasketPreviewProductPayment {
    val priceSummary = cssSelector(".basket-preview__product__payment")
    val currencyOverride = cssSelector(".js-checkout-currency-override")
  }
}
