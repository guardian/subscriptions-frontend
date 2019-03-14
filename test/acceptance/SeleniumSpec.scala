package acceptance

import acceptance.pages.{Checkout, ThankYou, WeeklyPromo}
import acceptance.util._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

// All Selenium tests should be placed in this spec, to avoid the overhead of spinning up multiple remote browser sessions.
// Only critical functionality should be tested with Selenium; prefer unit testing if possible.

class SeleniumSpec extends FeatureSpec with Browser
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { /* each test */ Driver.reset() }

  override def beforeAll() = {
    Config.printSummary()
  }

  override def afterAll() = {
    Driver.quit()
  }

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.SubscriptionFrontend.isAvailable,
      s"- ${Dependencies.SubscriptionFrontend.url} unavaliable! " +
        "\nPlease run subscriptions-frontend server before running tests.")

    assume(Dependencies.IdentityFrontend.isAvailable,
      s"- ${Dependencies.IdentityFrontend.url} unavaliable! " +
        "\nPlease run identity-frontend server before running tests.")
  }

  /* Register a new Identity user */
  def withRegisteredIdentityUserFixture(testFun: TestUser => Any) {
    checkDependenciesAreAvailable

    val testUser = new TestUser

    Given("that a user visits the 'Identity Registration' page")
    val registerStepOne = new pages.RegisterStepOne(testUser)
    go.to(registerStepOne)
    assert(registerStepOne.hasLoaded())

    When("they fill in the email address field")
    registerStepOne.fillInEmail()

    And("they submit the form")
    registerStepOne.submit()

    Then("they should be redirected to register as an Identity user")
    val registerStepTwo = new pages.RegisterStepTwo(testUser)
    assert(registerStepTwo.hasLoaded)

    Given("that the user fills in their personal details correctly")
    registerStepTwo.fillInPersonalDetails()

    When("they submit the form to create their Identity account")
    registerStepTwo.submit()

    // They should be a signed-in Identity user

    testFun(testUser)
  }

  feature("Subscriptions checkouts") {

    scenario("Guest user subscribes for the Digital Pack with direct debit", AcceptanceTest) {
      checkDependenciesAreAvailable
      val testUser = new TestUser

      val checkout = Checkout(testUser)
      When("Users visit 'Checkout' page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("section 'Your details' should load.")
      assert(checkout.yourDetailsSectionHasLoaded())

      When("they fill in personal details,")
      checkout.fillInPersonalDetails()

      And("click on 'Continue' button,")
      checkout.clickPersonalDetailsContinueButton()

      Then("section 'Billing Address' should load.")
      assert(checkout.billingAddressSectionHasLoaded())

      When("they fill in their billing address")
      checkout.fillInBillingAddress()

      And("click on 'Continue' button,")
      checkout.clickBillingDetailsContinueButton()

      Then("section 'Payment Details' should load.")
      assert(checkout.directDebitSectionHasLoaded())

      When("they fill in direct debit payment details")
      checkout.fillInDirectDebitPaymentDetails()

      And("select 'Confirm account holder' checkbox,")
      checkout.selectConfirmAccountHolder()

      And("click on 'Continue' button,")
      checkout.clickDebitPaymentContinueButton()

      Then("section 'Confirm and Review' should load.")
      assert(checkout.reviewSectionHasLoaded())

      When("they submit the form")
      checkout.submitPayment()

      Then("they should land on 'Thank You' page.")
      val thankYou = ThankYou(testUser)
      assert(thankYou.pageHasLoaded())

      When("they set a password for their account,")
      thankYou.setPassword("supers4af3passw0rd")

      Then("they should see 'My Profile' button,")
      assert(thankYou.hasMyProfileButton)

      And("should have Identity cookies.")
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(Driver.cookiesSet.map(_.getName).contains(idCookie))
      }
    }

    scenario("Identity user subscribes to the Voucher Everyday package with direct debit", AcceptanceTest) {
      withRegisteredIdentityUserFixture { testUser =>

        Given("a registered and signed in Identity user selects the Everyday package")

        Then("they should land on the 'Voucher Everyday Checkout' page,")
        val checkout = Checkout(testUser, "checkout/voucher-everyday")
        go.to(checkout)
        assert(checkout.pageHasLoaded())

        And("they should be signed in with their Identity account,")
        assert(checkout.userIsSignedIn)

        And("they should have Identity cookies,")
        Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
          assert(Driver.cookiesSet.map(_.getName).contains(idCookie))
        }

        And("the section 'Your details' should load.")
        assert(checkout.yourDetailsSectionHasLoaded())

        And("first name, last name, and email address should be pre-filled,")
        assert(checkout.userDetailsArePrefilled)

        When("they click the continue button")
        checkout.clickPersonalDetailsContinueButton()

        Then("the section 'Delivery Address' should load.")
        assert(checkout.deliveryAddressSectionHasLoaded())

        When("they fill in delivery address details,")
        checkout.fillInDeliveryAddressDetails()

        And("click on 'Continue' button,")
        checkout.clickDeliveryAddressDetailsContinueButton()

        Then("section 'Billing Address' should load.")
        assert(checkout.billingAddressSectionHasLoaded())

        Given("checkbox 'Bill my delivery address' is pre-selected,")

        When("they click on 'Continue' button,")
        checkout.clickBillingDetailsContinueButton()

        Then("section 'Payment Details' should load.")
        assert(checkout.directDebitSectionHasLoaded())

        When("they fill in direct debit payment details,")
        checkout.fillInDirectDebitPaymentDetails()

        And("select 'Confirm account holder' checkbox,")
        checkout.selectConfirmAccountHolder()

        And("click on 'Continue' button,")
        checkout.clickDebitPaymentContinueButton()

        Then("section 'Review and Confirm' should load.")
        assert(checkout.reviewSectionHasLoaded())

        When("they click on 'Submit payment' button,")
        checkout.submitPayment()

        Then("they should land on 'Thank You' page.")
        val thankYou = ThankYou(testUser)
        assert(thankYou.pageHasLoaded())

        And("they should still be signed in.")
        assert(thankYou.userIsSignedIn)
      }
    }

    scenario("Weekly quarterly sub purchase from UK for UK delivery", AcceptanceTest) {
      checkDependenciesAreAvailable
      val testUser = new TestUser
      val checkout = Checkout(testUser, "checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=uk")
      When("User hits checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("the 'Product Payment' section should load")
      assert(checkout.basketPreviewProductPaymentHasLoaded())

      And("the payment amount and currency should be correct")
      assert(checkout.basketPreviewProductPaymentContains("£37.50 every 3 months"))

      And("the 'Pay in GBP' override should NOT load")
      assert(!checkout.currencyOverrideHasLoaded())
    }

    scenario("Weekly quarterly sub purchase from EU for EU delivery", AcceptanceTest) {
      checkDependenciesAreAvailable
      val testUser = new TestUser
      val checkout = Checkout(testUser, "checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=eu")
      When("User hits checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("the 'Product Payment' section should load")
      assert(checkout.basketPreviewProductPaymentHasLoaded())

      And("the payment amount and currency should be correct")
      assert(checkout.basketPreviewProductPaymentContains("€61.30 every 3 months"))

      And("the 'Pay in GBP' override should NOT load")
      assert(!checkout.currencyOverrideHasLoaded())
    }

    scenario("Weekly quarterly sub purchase for ROW delivery", AcceptanceTest) {
      checkDependenciesAreAvailable
      val testUser = new TestUser
      val checkout = Checkout(testUser, "checkout/weeklyrestofworld-gwoct18-quarterly-row?countryGroup=us")
      When("User hits checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("the 'Product Payment' section should load")
      assert(checkout.basketPreviewProductPaymentHasLoaded())

      And("the payment amount and currency should be correct")
      assert(checkout.basketPreviewProductPaymentContains("US$81.30 every 3 months"))

      And("the 'Pay in GBP' override should load")
      assert(checkout.currencyOverrideHasLoaded())
    }

    scenario("Weekly quarterly sub purchase from ROW for UK delivery", AcceptanceTest) {
      checkDependenciesAreAvailable
      val testUser = new TestUser
      val checkout = Checkout(testUser, "checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=uk")
      When("User hits checkout page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("the 'Product Payment' section should load")
      assert(checkout.basketPreviewProductPaymentHasLoaded())

      And("the payment amount and currency should be correct")
      assert(checkout.basketPreviewProductPaymentContains("£37.50 every 3 months"))

      And("the 'Pay in GBP' override should NOT load")
      assert(!checkout.currencyOverrideHasLoaded())
    }
  }
}
