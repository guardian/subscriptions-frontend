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

    scenario("UK user lands on Guardian Weekly promotion page", AcceptanceTest) {
      val promoCode = "/p/10ANNUAL"

      When("The user is in the UK")
      val promoPage = WeeklyPromo(endpoint = promoCode)

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
        0,
        "/checkout/weeklyzonea-guardianweeklyquarterly?countryGroup=uk")
      )

      And("the quarterly title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "UnitedKingdom",
        0,
        "Quarterly")
      )

      And("the quarterly description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "UnitedKingdom",
        0,
        "£30 every 3 months")
      )

      And("the annual link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "UnitedKingdom",
        1,
        "/checkout/weeklyzonea-guardianweeklyannual?countryGroup=uk&promoCode=10ANNUAL")
      )

      And("the annual title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "UnitedKingdom",
        1,
        "Annual")
      )

      And("the annual description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "UnitedKingdom",
        1,
        "£108 for 1 year, then standard rate (£120 every year)")
      )

    }

    scenario("ZA user lands on GW promo page", AcceptanceTest) {
      val promoCode = "/p/10ANNUAL"

      When("The user is in South Africa")
      val promoPage = WeeklyPromo(endpoint = promoCode, country = "ZA")

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
        0,
        "/checkout/weeklyzonec-guardianweeklyquarterly?countryGroup=us")
      )

      And("the quarterly title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "SouthAfrica",
        0,
        "Quarterly")
      )

      And("the quarterly description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "SouthAfrica",
        0,
        "US$65 every 3 months")
      )

      And("the annual link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "SouthAfrica",
        1,
        "/checkout/weeklyzonec-guardianweeklyannual?countryGroup=us&promoCode=10ANNUAL")
      )

      And("the annual title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "SouthAfrica",
        1,
        "Annual")
      )

      And("the annual description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "SouthAfrica",
        1,
        "US$234 for 1 year, then standard rate (US$260 every year)")
      )

    }

    // NEW PRICE/PRODUCT TEST

    scenario("UK user lands on Guardian Weekly promotion page AFTER 10:45 on 10/10/18", AcceptanceTest) {
      val promoCode = "/p/10ANNUAL"
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
        0,
        "/checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=uk")
      )

      And("the quarterly title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "UnitedKingdom",
        0,
        "Quarterly")
      )

      And("the quarterly description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "UnitedKingdom",
        0,
        "£37.50 every 3 months")
      )

      And("the annual link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "UnitedKingdom",
        1,
        "/checkout/weeklydomestic-gwoct18-annual-domestic?countryGroup=uk")
      )

      And("the annual title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "UnitedKingdom",
        1,
        "Annual")
      )

      And("the annual description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "UnitedKingdom",
        1,
        "£150 every 12 months")
      )

    }

    scenario("FR user lands on Guardian Weekly promotion page AFTER 10:45 on 10/10/18", AcceptanceTest) {
      val promoCode = "/p/10ANNUAL"
      val country = "FR"
      val params = Some("gwoct18")

      When("The user is in FRANCE")
      val promoPage = WeeklyPromo(endpoint = promoCode, country=country, params = params)

      go.to(promoPage)
      assert(promoPage.pageHasLoaded())

      Then("United Kingdom should be the first list item")
      assert(promoPage.DestinationList.firstElementAttribute("data-dropdown-menu") === "UnitedKingdom")

      When("they click for delivery to EUROPE")
      promoPage.DestinationList.chooseEuropeDelivery()

      Then("the correct menu should become visible")
      assert(promoPage.DestinationList.menuIsVisible("Europe"))

      And("the quarterly link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "Europe",
        0,
        "/checkout/weeklydomestic-gwoct18-quarterly-domestic?countryGroup=eu")
      )

      And("the quarterly title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "Europe",
        0,
        "Quarterly")
      )

      And("the quarterly description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "Europe",
        0,
        "€61.30 every 3 months")
      )

      And("the annual link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "Europe",
        1,
        "/checkout/weeklydomestic-gwoct18-annual-domestic?countryGroup=eu")
      )

      And("the annual title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "Europe",
        1,
        "Annual")
      )

      And("the annual description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "Europe",
        1,
        "€245.20 every 12 months")
      )

    }

    scenario("ZA user lands on GW promo page AFTER 10:45 on 10/10/18", AcceptanceTest) {
      val promoCode = "/p/10ANNUAL"
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
        0,
        "/checkout/weeklyrestofworld-gwoct18-quarterly-row?countryGroup=us")
      )

      And("the quarterly title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "SouthAfrica",
        0,
        "Quarterly")
      )

      And("the quarterly description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "SouthAfrica",
        0,
        "US$81.30 every 3 months")
      )

      And("the annual link should be correct")
      assert(promoPage.DestinationList.menuContainsLink(
        "SouthAfrica",
        1,
        "/checkout/weeklyrestofworld-gwoct18-annual-row?countryGroup=us")
      )

      And("the annual title should be correct")
      assert(promoPage.DestinationList.menuContainsTitle(
        "SouthAfrica",
        1,
        "Annual")
      )

      And("the annual description should be correct")
      assert(promoPage.DestinationList.menuContainsDescription(
        "SouthAfrica",
        1,
        "US$325.20 every 12 months")
      )

    }

  }
}
