package acceptance.pages

import acceptance.util.{Browser, Config, Driver, TestUser}
import Config.baseUrl
import org.openqa.selenium.By
import org.scalatest.selenium.Page
import views.fragments.ABTest

case class Checkout(testUser: TestUser, endpoint: String = "checkout") extends Page with Browser {
  val url = s"$baseUrl/$endpoint"

  def fillInPersonalDetails(): Unit = PersonalDetails.fillIn()

  def clickPersonalDetailsContinueButton() = PersonalDetails.continue()

  def deliveryAddressSectionHasLoaded(): Boolean = pageHasElement(DeliveryAddress.continueButton)

  def fillInDeliveryAddressDetails(): Unit = DeliveryAddress.fillIn()

  def clickDeliveryAddressDetailsContinueButton() = DeliveryAddress.continue()

  def clickBillingDetailsContinueButton() = BillingAddress.continue()

  def fillInBillingAddress(): Unit = BillingAddress.fillIn()

  def fillInDirectDebitPaymentDetails(): Unit = DebitCardPaymentDetails.fillIn()

  def selectConfirmAccountHolder(): Unit = DebitCardPaymentDetails.confirm.select()

  def clickDebitPaymentContinueButton(): Unit = DebitCardPaymentDetails.continue()

  def fillInCreditCardPaymentDetailsStripe(): Unit = StripeCheckout.fillIn()

  def clickCredictCardPaymentContinueButton() = CreditCardPaymentDetails.continue()

  def selectCreditCardPaymentOption() = setRadioButtonValue(CreditCardPaymentDetails.paymentType, "card")

  def submitPayment(): Unit = clickOn(submitPaymentButton)

  def pageHasLoaded(): Boolean = pageHasElement(cssSelector(".js-checkout-your-details-submit"))

  def yourDetailsSectionHasLoaded(): Boolean = pageHasElement(PersonalDetails.continueButton)

  def directDebitSectionHasLoaded(): Boolean = pageHasElement(DebitCardPaymentDetails.continueButton)

  def billingAddressSectionHasLoaded(): Boolean = pageHasElement(BillingAddress.continueButton)

  def cardSectionHasLoaded(): Boolean = pageHasElement(CreditCardPaymentDetails.continueButton)

  def switchToStripe() = driver.switchTo().frame(driver.findElement(StripeCheckout.container.by))

  def stripeCheckoutHasLoaded(): Boolean = pageHasElement(StripeCheckout.container)

  def stripeCheckoutHasCC(): Boolean = pageHasElement(StripeCheckout.cardNumber)

  def stripeCheckoutHasCVC(): Boolean = pageHasElement(StripeCheckout.cardCvc)

  def stripeCheckoutHasExph(): Boolean = pageHasElement(StripeCheckout.cardExp)

  def stripeCheckoutHasSubmit(): Boolean = pageHasElement(StripeCheckout.submitButton)

  def reviewSectionHasLoaded(): Boolean = pageHasElement(ReviewSection.submitPaymentButton)

  def userIsSignedIn: Boolean = elementHasText(userDisplayName, testUser.username.toLowerCase)

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
    val continueButton = cssSelector(".js-checkout-payment-details-submit")

    def continue(): Unit = clickOn(continueButton)
  }

  private object ReviewSection {
    val submitPaymentButton = cssSelector(".js-checkout-submit")
  }

  // Temporary hack to identify elements on Stripe Checkout form using xpath, since the ids are no longer consistently set.
  private object StripeCheckout {
    val container = name("stripe_checkout_app")
    val cardNumber = xpath("//div[label/text() = \"Card number\"]/input")
    val cardExp = xpath("//div[label/text() = \"Expiry\"]/input")
    val cardCvc = xpath("//div[label/text() = \"CVC\"]/input")
    val submitButton = xpath("//div[button]")

    private def fillInHelper(cardNum: String) = {

      setValueSlowly(cardNumber, cardNum)
      setValueSlowly(cardExp, "1019")
      setValueSlowly(cardCvc, "111")
      continue()
      Thread.sleep(5000)
    }

    /*
    * Stripe wants you to pause between month and year and between each quartet in the cc
    * This causes pain when you use Selenium. There are a few stack overflow posts- but nothing really useful.
    * */
    private def setValueSlowly(q: Query, value: String): Unit = {
      for {
        c <- value
      } yield {
        setValue(q, c.toString)
        Thread.sleep(100)
      }
    }

    def fillIn(): Unit = fillInHelper("4242 4242 4242 4242")

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

    def continue(): Unit = clickOn(submitButton)
  }
}
