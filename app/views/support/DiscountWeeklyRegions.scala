package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.Subscription
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}
import com.gu.memsub.subsv2.{Catalog, CatalogPlan}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support.Pricing._
//TODO: move this and revert to the old

case class DiscountWeeklyRegion(title: String, description: String, url: Uri, ratePlans: Set[Subscription.ProductRatePlanId], countries: Set[Country]) {
}

object DiscountWeeklyRegion {

  private val UK = CountryGroup.UK.countries.toSet
  private val US = CountryGroup.US.countries.toSet
  private val ZONEC = CountryGroup.allGroups.flatMap(_.countries).toSet.diff(UK union US)

  def all(catalog: Catalog): List[DiscountWeeklyRegion] = List(DiscountWeeklyRegion(
    title = "United Kingdom",
    description = "Includes Isle of Man and Channel Islands",
    url = Uri.parse(s"checkout/${catalog.weeklyZoneA.quarter.slug}").addParam("countryGroup", "uk"),
    ratePlans = catalog.weeklyZoneA.toList.map(_.id).toSet,
    countries = UK
  ), DiscountWeeklyRegion(
    title = "United States",
    description = "Includes Alaska and Hawaii",
    url = Uri.parse(s"checkout/${catalog.weeklyZoneA.quarter.slug}").addParam("countryGroup", "us"),
    ratePlans = catalog.weeklyZoneA.toList.map(_.id).toSet,
    countries = US
  ), DiscountWeeklyRegion(
    title = "Rest of the world",
    description = "Posted to you by air mail",
    url = Uri.parse(s"checkout/${catalog.weeklyZoneC.quarter.slug}"),
    ratePlans = catalog.weeklyZoneC.toList.map(_.id).toSet,
    countries = ZONEC
  ))

  def forPromotion(promotion: PromoWithWeeklyLandingPage, catalog: Catalog) = {
    val promotionProductRatePlanIds = promotion.appliesTo.productRatePlanIds
    val promotionCountries = promotion.appliesTo.countries

    all(catalog).filter { region =>
      val ratePlaninPromotion = promotionProductRatePlanIds.exists(plan => region.ratePlans.contains(plan))
      val countryInPromotion = !region.countries.intersect(promotionCountries).isEmpty
      ratePlaninPromotion && countryInPromotion
    }
  }

  private val currencyFor = CountryGroup.availableCurrency(Currency.all.toSet) _

  case class discountedPlan(pretty: String, period: String, url: Uri)

  def plans(promotion: PromoWithWeeklyLandingPage, region: DiscountWeeklyRegion, catalog: Catalog, promoCode: PromoCode): List[discountedPlan] = {
    def availableCountries = (region.countries intersect promotion.appliesTo.countries).toList

    val cs = availableCountries.flatMap(currencyFor(_))

    def plans: List[CatalogPlan.Paid] = catalog.weekly.flatten.filter(plan => promotion.appliesTo.productRatePlanIds.contains(plan.id))

    for {
      plan <- plans
      discountPromo <- promotion.asDiscount.toList
      currency <- plan.charges.currencies.toList
      if (cs.contains(currency))
    } yield {
      discountedPlan(
        pretty = plan.charges.prettyPricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency),
        period = plan.charges.billingPeriod.noun,
        url = s"checkout/${plan.slug}" ? ("promoCode" -> promoCode.get) & ("countryGroup" -> CountryGroup.allGroups.find(_.currency == currency).getOrElse(CountryGroup.UK).id))
    }
  }
}




