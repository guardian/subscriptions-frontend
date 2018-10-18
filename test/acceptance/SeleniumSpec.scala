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

    // When Users visit 'Identity Register' page
    val register = new pages.Register(testUser)
    go.to(register)
    assert(register.hasLoaded())

    // And they fill in personal details
    register.fillInPersonalDetails()

    // And they submit the form to create Identity account
    register.createAccount()
    assert(register.hasCreated())

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

    ignore("Identity user subscribes to the Voucher Everyday package with direct debit", AcceptanceTest) {
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

  feature("Weekly Promotion Page") {

      scenario("UK user lands on Guardian Weekly promotion page", AcceptanceTest) {
        val promoCode = "/p/WWM99X"
        val params = Some("gwoct18")

        When("The user is in the UK")
        val promoPage = WeeklyPromo(endpoint = promoCode, params = params)

        go.to(promoPage)
        assert(promoPage.pageHasLoaded())

        Then("United Kingdom should be the first list item")
        assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "UnitedKingdom")

        When("they click for delivery to the UK")
        promoPage.DestinationList.chooseLocalDelivery()

        Then("the correct menu should become visible")
        assert(promoPage.DestinationList.menuIsVisible("UnitedKingdom"))

        And("the quarterly link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "UnitedKingdom",
          1,
          "/checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=uk")
        )

        And("the quarterly title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "UnitedKingdom",
          1,
          "Quarterly")
        )

        And("the quarterly description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "UnitedKingdom",
          1,
          "£37.50 every 3 months")
        )

        And("the annual link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "UnitedKingdom",
          2,
          "/checkout/weeklydomestic-gwoct18-annual-domestic?countryGroup=uk")
        )

        And("the annual title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "UnitedKingdom",
          2,
          "Annual")
        )

        And("the annual description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "UnitedKingdom",
          2,
          "£150 every 12 months")
        )

      }

      scenario("FR user lands on Guardian Weekly promotion page", AcceptanceTest) {
        val promoCode = "/p/WWM99X"
        val country = "FR"
        val params = Some("gwoct18")

        When("The user is in FRANCE")
        val promoPage = WeeklyPromo(endpoint = promoCode, country=country, params = params)

        go.to(promoPage)
        assert(promoPage.pageHasLoaded())

        Then("Europe should be the first list item")
        assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "Europe")

        When("they click for delivery to Europe")
        promoPage.DestinationList.chooseLocalDelivery()

        Then("the correct menu should become visible")
        assert(promoPage.DestinationList.menuIsVisible("Europe"))

        And("the quarterly link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "Europe",
          1,
          "/checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=eu")
        )

        And("the quarterly title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "Europe",
          1,
          "Quarterly")
        )

        And("the quarterly description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "Europe",
          1,
          "€61.30 every 3 months")
        )

        And("the annual link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "Europe",
          2,
          "/checkout/weeklydomestic-gwoct18-annual-domestic?countryGroup=eu")
        )

        And("the annual title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "Europe",
          2,
          "Annual")
        )

        And("the annual description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "Europe",
          2,
          "€245.20 every 12 months")
        )

      }

      scenario("ZA user lands on GW promo page", AcceptanceTest) {
        val promoCode = "/p/WWM99X"
        val country = "ZA"
        val params = Some("gwoct18")

        When("The user is in South Africa")
        val promoPage = WeeklyPromo(endpoint = promoCode, country = "ZA", params = params)

        go.to(promoPage)
        assert(promoPage.pageHasLoaded())

        Then("South Africa should be the first list item")
        assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "SouthAfrica")

        When("they click for delivery to South Africa")
        promoPage.DestinationList.chooseLocalDelivery()

        Then("the correct menu should become visible")
        assert(promoPage.DestinationList.menuIsVisible("SouthAfrica"))

        And("the quarterly link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "SouthAfrica",
          1,
          "/checkout/weeklyrestofworld-gwoct18-quarterly-row?countryGroup=us")
        )

        And("the quarterly title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "SouthAfrica",
          1,
          "Quarterly")
        )

        And("the quarterly description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "SouthAfrica",
          1,
          "US$81.30 every 3 months")
        )

        And("the annual link should be correct")
        assert(promoPage.DestinationList.menuContainsLink(
          "SouthAfrica",
          2,
          "/checkout/weeklyrestofworld-gwoct18-annual-row?countryGroup=us")
        )

        And("the annual title should be correct")
        assert(promoPage.DestinationList.menuContainsTitle(
          "SouthAfrica",
          2,
          "Annual")
        )

        And("the annual description should be correct")
        assert(promoPage.DestinationList.menuContainsDescription(
          "SouthAfrica",
          2,
          "US$325.20 every 12 months")
        )
      }
    }
}
