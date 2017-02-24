package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.Subscription
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.subsv2.{Catalog, CatalogPlan}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support.Pricing._
import com.gu.memsub.promo.{PromoCode, Promotion, WeeklyLandingPage}

object WeeklyPromotion {
  private val UK = CountryGroup.UK.countries.toSet
  private val UKdomestic = Set(CountryGroup.C.UK)
  private val UKoverseas = UK diff UKdomestic
  private val US = CountryGroup.US.countries.toSet
  private val ZONEC = CountryGroup.allGroups.flatMap(_.countries).toSet.diff(UK union US)
  case class DiscountedPlan(pretty: String, headline: String, period: String, url: Uri)

  case class DiscountedRegion(title: String, description: String, countries: Set[Country], discountedPlans: List[DiscountedPlan])

  def validRegionsForPromotion(promotion: PromoWithWeeklyLandingPage, promoCode: PromoCode, requestCountry: Country)(implicit catalog: Catalog): Seq[DiscountedRegion] = {
    val promotionProductRatePlanIds = promotion.appliesTo.productRatePlanIds
    val promotionCountries = promotion.appliesTo.countries
    val regionForZoneCCountry: Seq[DiscountedRegion] = {
      if (ZONEC.contains(requestCountry)) {
        Seq(DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          countries = Set(requestCountry),
          discountedPlans = plansForPromotion(promotion, promoCode, Set(requestCountry), catalog.weeklyZoneC.toList)
        ))
      } else {
        Seq()
      }
    }
    val rowWithoutCountry: DiscountedRegion = {
      DiscountedRegion(
        title = "Rest of the world",
        description = "Posted to you by air mail",
        countries = ZONEC - requestCountry,
        discountedPlans = plansForPromotion(promotion, promoCode, ZONEC - requestCountry, catalog.weeklyZoneC.toList)
      )
    }
    val UKregion: Set[DiscountedRegion] = {
      val all = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes Isle of Man and Channel Islands",
        countries = UK,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.toList)
      )
      val domestic = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes mainland UK only.",
        countries = UKdomestic,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.toList)
      )
      val overseas = DiscountedRegion(
        title = "Isle of Man and Channel Islands",
        description = "",
        countries = UKoverseas,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.toList)
      )
      val countries = promotionCountries intersect UK
      val isDomestic = !(countries intersect UKdomestic).isEmpty
      val isOverseas = !(countries intersect UKoverseas).isEmpty
      if(isDomestic && isOverseas){
        Set(all)
      } else if (isDomestic) {
        Set(domestic)
      } else if (isOverseas) {
        Set(overseas)
      } else {
        Set()
      }
    }
    val USregion = DiscountedRegion(
      title = "United States",
      description = "Includes Alaska and Hawaii",
      countries = US,
      discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.toList)
    )
    val regions = regionForZoneCCountry ++ UKregion ++ Seq(USregion) ++ Seq(rowWithoutCountry)
    regions.filter(_.discountedPlans.length > 0)
  }

  private val currencyFor = CountryGroup.availableCurrency(Currency.all.toSet) _

  def plansForPromotion(promotion: PromoWithWeeklyLandingPage, promoCode: PromoCode, countries: Set[Country], catalogPlans: Seq[CatalogPlan.Paid])(implicit catalog: Catalog): List[DiscountedPlan] = {
    def currencies = (countries intersect promotion.appliesTo.countries).flatMap { country => currencyFor(country) }

    def plans: List[CatalogPlan.Paid] =
      catalogPlans.filter(catalogPlan => promotion.appliesTo.productRatePlanIds.contains(catalogPlan.id)).toList.sortBy(_.charges.gbpPrice.amount)

    for {
      plan <- plans
      currency <- plan.charges.currencies.toList
      if (currencies.contains(currency))
    } yield {
      val pretty = promotion.asDiscount
        .map { discountPromo => plan.charges.prettyPricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency) }
        .getOrElse(plan.charges.prettyPricing(currency))
      val headline = promotion.asDiscount
        .map { discountPromo => plan.charges.headlinePricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency) }.getOrElse(plan.charges.prettyPricing(currency))
      DiscountedPlan(
        pretty = pretty,
        headline = headline,
        period = plan.charges.billingPeriod.adjective,
        url = s"checkout/${plan.slug}" ? ("promoCode" -> promoCode.get) & ("countryGroup" -> CountryGroup.allGroups.find(_.currency == currency).getOrElse(CountryGroup.UK).id))
    }
  }

}
