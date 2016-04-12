package acceptance

import acceptance.pages.{Checkout, ThankYou}
import acceptance.util._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class CheckoutSpec extends FeatureSpec with Browser
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { /* each test */ Driver.reset() }

  override def beforeAll() = {
    Screencast.storeId()
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

    testFun(testUser)
  }

  feature("Subscription checkout") {
    scenario("Guest users subscribe with direct debit", Acceptance) {
      checkDependenciesAreAvailable
      val testUser = new TestUser

      val checkout = Checkout(testUser)
      When("Users visit 'Checkout' page ")
      go.to(checkout)
      assert(checkout.pageHasLoaded())

      Then("section 'Your details' should load.")
      assert(checkout.yourDetailsSectionHasLoaded)

      When("they fill in personal details,")
      checkout.fillInPersonalDetails()

      And("click on 'Continue' button,")
      checkout.clickPersonalDetailsContinueButton()

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
        assert(Driver.cookiesSet.map(_.getName).contains(idCookie)) }
    }

    scenario("Identity user subscribes with credit card", Acceptance) {
      withRegisteredIdentityUserFixture { testUser =>

        Given("registered and signed in Identity users want to subscribe by clicking on " +
          "'Start your free trial'")

        Then("they should land on 'Checkout' page,")
        val checkout = pages.Checkout(testUser)
        go.to(checkout)
        assert(checkout.pageHasLoaded())

        And("should be signed in with their Identity account,")
        assert(checkout.userIsSignedIn)

        And("first name, last name, and email address should be pre-filled,")
        assert(checkout.userDetailsArePrefilled)

        And("they should have Identity cookies,")
        Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
          assert(Driver.cookiesSet.map(_.getName).contains(idCookie))
        }

        And("the section 'Your details' should load.")
        assert(checkout.yourDetailsSectionHasLoaded)

        When("they fill in the address,")
        checkout.fillInAddressDetails()

        And("click on 'Continue' button,")
        checkout.clickPersonalDetailsContinueButton()

        Then("the section 'Payment Details' should load.")
        assert(checkout.directDebitSectionHasLoaded())

        When("Users select credit card payment option,")
        checkout.selectCreditCardPaymentOption()

        And("fill in credit card payment details,")
        checkout.fillInCreditCardPaymentDetails()

        And("click on 'Continue' button,")
        checkout.clickCredictCardPaymentContinueButton()

        Then("the section 'Confirm and Review' should load.")
        assert(checkout.reviewSectionHasLoaded())

        When("they submit the form,")
        checkout.submitPayment()

        Then("they should land on 'Thank You' page,")
        val thankYou = ThankYou(testUser)
        assert(thankYou.pageHasLoaded())

        And("they should still be signed in.")
        assert(thankYou.userIsSignedIn)
      }
    }
  }
}
