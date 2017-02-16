package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.Subscription
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.promo.{PromoCode, Promotion, WeeklyLandingPage}
import com.gu.memsub.subsv2.{Catalog, CatalogPlan}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support.Pricing._

object DiscountWeeklyRegions {
  case class discountedPlan(pretty: String, headline: String, period: String, url: Uri)
  case class DiscountWeeklyRegion(title: String, description: String, ratePlans: Set[Subscription.ProductRatePlanId], countries: Set[Country]) {
    def promotionalRatePlans(implicit promotion: PromoWithWeeklyLandingPage) = {
      ratePlans intersect promotion.appliesTo.productRatePlanIds
    }
  }

  private val UK = CountryGroup.UK.countries.toSet
  private val US = CountryGroup.US.countries.toSet
  private val ZONEC = CountryGroup.allGroups.flatMap(_.countries).toSet.diff(UK union US)

  def regionForZoneCCountry(country: Country)(implicit catalog: Catalog): Option[DiscountWeeklyRegion] = {
    if (ZONEC.contains(country)) {
      Some(DiscountWeeklyRegion(
        title = country.name,
        description = "Posted to you by air mail",
        ratePlans = catalog.weeklyZoneC.toList.map(_.id).toSet,
        countries = Set(country)
      ))
    } else {
      None
    }
  }

  def rowWithoutCountry(country: Country)(implicit catalog: Catalog): DiscountWeeklyRegion = {
    DiscountWeeklyRegion(
      title = "Rest of the world",
      description = "Posted to you by air mail",
      ratePlans = catalog.weeklyZoneC.toList.map(_.id).toSet,
      countries = ZONEC - country
    )
  }

  def regionList(country: Country)(implicit catalog: Catalog): Iterable[DiscountWeeklyRegion] = {
    val UKandUS = Seq(DiscountWeeklyRegion(
      title = "United Kingdom",
      description = "Includes Isle of Man and Channel Islands",
      ratePlans = catalog.weeklyZoneA.toList.map(_.id).toSet,
      countries = UK
    ), DiscountWeeklyRegion(
      title = "United States",
      description = "Includes Alaska and Hawaii",
      ratePlans = catalog.weeklyZoneA.toList.map(_.id).toSet,
      countries = US
    ))
    regionForZoneCCountry(country) ++ UKandUS ++ Seq(rowWithoutCountry(country))
  }

  def forPromotion(promotion: PromoWithWeeklyLandingPage, country: Country)(implicit catalog: Catalog) = {
    val promotionProductRatePlanIds = promotion.appliesTo.productRatePlanIds
    val promotionCountries = promotion.appliesTo.countries
    regionList(country).filter { region =>
      val ratePlaninPromotion = promotionProductRatePlanIds.exists(plan => region.ratePlans.contains(plan))
      val countryInPromotion = !region.countries.intersect(promotionCountries).isEmpty
      ratePlaninPromotion && countryInPromotion
    }
  }

  private val currencyFor = CountryGroup.availableCurrency(Currency.all.toSet) _

  def plans(promotion: PromoWithWeeklyLandingPage, region: DiscountWeeklyRegion, catalog: Catalog, promoCode: PromoCode): List[discountedPlan] = {
    def availableCountries = (region.countries intersect promotion.appliesTo.countries).toList

    val cs = availableCountries.flatMap(currencyFor(_)).toSet
    def plans: List[CatalogPlan.Paid] = catalog.weekly.flatten.filter(plan => region.ratePlans.contains(plan.id)).filter(plan => promotion.appliesTo.productRatePlanIds.contains(plan.id))

    for {
      plan <- plans
      currency <- plan.charges.currencies.toList
      if (cs.contains(currency))
    } yield {
      discountedPlan(
        pretty = promotion.asDiscount
          .map{discountPromo => plan.charges.prettyPricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency)}
          .getOrElse(plan.charges.prettyPricing(currency)),
        headline = promotion.asDiscount
          .map{discountPromo => plan.charges.headlinePricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency)}
          .getOrElse(plan.charges.prettyPricing(currency)),
        period = plan.charges.billingPeriod.adjective,
        url = s"checkout/${plan.slug}" ? ("promoCode" -> promoCode.get) & ("countryGroup" -> CountryGroup.allGroups.find(_.currency == currency).getOrElse(CountryGroup.UK).id))
    }
  }

}
