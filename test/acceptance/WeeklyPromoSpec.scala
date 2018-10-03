package acceptance

import acceptance.pages.WeeklyPromo
import acceptance.util._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class WeeklyPromoSpec extends FeatureSpec with Browser
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { /* each test */ Driver.reset() }

  override def beforeAll() = {
    Config.printSummary()
    checkDependenciesAreAvailable
  }

  override def afterAll() = {
    Driver.quit()
  }

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.SubscriptionFrontend.isAvailable,
      s"- ${Dependencies.SubscriptionFrontend.url} unavaliable! " +
        "\nPlease run subscriptions-frontend server before running tests.")
  }

  feature("Weekly Promotion Page") {

    scenario("UK user lands on Guardian Weekly promotion page", Acceptance) {
      val promoCode = "/p/10ANNUAL"

      When("The user is in the UK")
      val promoPage = WeeklyPromo(endpoint = promoCode)

      go.to(promoPage)
      assert(promoPage.pageHasLoaded())

      Then("United Kingdom should be the first list item")
      assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "UnitedKingdom")

      When("they click for delivery to the UK")
      promoPage.DestinationList.chooseLocalDelivery()

// TODO!
//      Then("the correct menu should become visible")
//      assert(promoPage.DestinationList.menuIsOpened("UnitedKingdom"))

    }

    scenario("ZA user lands on GW promo page", Acceptance, Weekly) {
      val promoCode = "/p/10ANNUAL"

      When("The user is in South Africa")
      val promoPage = WeeklyPromo(endpoint = promoCode, country = "ZA")

      go.to(promoPage)
      assert(promoPage.pageHasLoaded())

      Then("South Africa should be the first list item")
      assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "SouthAfrica")

      When("they click for delivery to South Africa")
      promoPage.DestinationList.chooseLocalDelivery()

// TODO!
//      Then("the correct menu item should become visible")
//      assert(promoPage.DestinationList.menuIsOpened("SouthAfrica"))

    }

  }
}
