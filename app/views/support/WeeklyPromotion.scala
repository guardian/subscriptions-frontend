package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.Subscription
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.subsv2.{Catalog, CatalogPlan}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support.Pricing._
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}

object WeeklyPromotion {
  private val UK = CountryGroup.UK.countries.toSet
  private val UKdomestic = Set(CountryGroup.C.UK)
  private val UKoverseas = UK diff UKdomestic
  private val US = CountryGroup.US.countries.toSet
  private val AU = CountryGroup.Australia.countries.toSet
  private val NZ = CountryGroup.NewZealand.countries.toSet
  private val EU = CountryGroup.Europe.countries.toSet
  private val CA = CountryGroup.Canada.countries.toSet
  private val allCountries = CountryGroup.allGroups.flatMap(_.countries).toSet
  private val ZONEC = allCountries.toSet.diff(UK union US)
  case class DiscountedPlan(currency: Currency, pretty: String, headline: String, period: String, url: Uri, discounted: Boolean)

  case class DiscountedRegion(title: String, description: String, countries: Set[Country], discountedPlans: List[DiscountedPlan])

  def validRegionsForPromotion(promotion: Option[PromoWithWeeklyLandingPage], promoCode: Option[PromoCode], requestCountry: Country)(implicit catalog: Catalog): Seq[DiscountedRegion] = {
    val weekly = catalog.weekly.flatten.map(_.id).toSet
    val promotionProductRatePlanIds: Set[Subscription.ProductRatePlanId] = promotion.map(_.appliesTo.productRatePlanIds).getOrElse(weekly)
    val promotionCountries = promotion.map(_.appliesTo.countries).getOrElse(allCountries)
    val regionForZoneCCountry: Seq[DiscountedRegion] = {
      if (ZONEC.contains(requestCountry)) {
        Seq(DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          countries = Set(requestCountry),
          discountedPlans = plansForPromotion(promotion, promoCode, Set(requestCountry), catalog.weeklyZoneC.plans)
        ))
      } else {
        Seq()
      }
    }
    val rowWithoutCountry: Seq[DiscountedRegion] = Seq(DiscountedRegion(
        title = "Rest of the world",
        description = "Posted to you by air mail",
        countries = ZONEC - requestCountry,
        discountedPlans = plansForPromotion(promotion, promoCode, ZONEC - requestCountry, catalog.weeklyZoneC.plans)
      ))

    val UKregion: Set[DiscountedRegion] = {
      val all = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes Isle of Man and Channel Islands",
        countries = UK,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.plans)
      )
      val domestic = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes mainland UK only.",
        countries = UKdomestic,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.plans)
      )
      val overseas = DiscountedRegion(
        title = "Isle of Man and Channel Islands",
        description = "",
        countries = UKoverseas,
        discountedPlans = plansForPromotion(promotion, promoCode, UK, catalog.weeklyZoneA.plans)
      )
      val countries = promotionCountries intersect UK
      val includesUKDomestic = !(countries intersect UKdomestic).isEmpty
      val includesUKOverseas = !(countries intersect UKoverseas).isEmpty
      if(includesUKDomestic && includesUKOverseas){
        Set(all)
      } else if (includesUKDomestic) {
        Set(domestic)
      } else if (includesUKOverseas) {
        Set(overseas)
      } else {
        Set()
      }
    }
    val USregion = Seq(DiscountedRegion(
      title = "United States",
      description = "Includes Alaska and Hawaii",
      countries = US,
      discountedPlans = plansForPromotion(promotion, promoCode, US, catalog.weeklyZoneA.plans)
    ))
    val AUSregion =  if (AU contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Australia",
      description = "Posted to you by air mail",
      countries = AU,
      discountedPlans = plansForPromotion(promotion, promoCode, AU, catalog.weeklyZoneC.plans)
    ))
    val NZregion =  if (NZ contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "New Zealand",
      description = "Posted to you by air mail",
      countries = NZ,
      discountedPlans = plansForPromotion(promotion, promoCode, NZ, catalog.weeklyZoneC.plans)
    ))
    val CAregion =  if (CA contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Canada",
      description = "Posted to you by air mail",
      countries = CA,
      discountedPlans = plansForPromotion(promotion, promoCode, CA, catalog.weeklyZoneC.plans)
    ))
    val EUregion = Seq(DiscountedRegion(
      title = "Europe",
      description = "Posted to you by air mail",
      countries = EU,
      discountedPlans = plansForPromotion(promotion, promoCode, EU, catalog.weeklyZoneC.plans)
    ))
    val regions = regionForZoneCCountry ++ UKregion ++ USregion ++ EUregion ++ AUSregion ++  NZregion ++ CAregion  ++ rowWithoutCountry
    regions.filter(_.discountedPlans.length > 0)
  }

  private val currencyFor = CountryGroup.availableCurrency(Currency.all.toSet) _

  def plansForPromotion(promotion: Option[PromoWithWeeklyLandingPage], promoCode: Option[PromoCode], countries: Set[Country], catalogPlans: Seq[CatalogPlan.Paid])(implicit catalog: Catalog): List[DiscountedPlan] = {
    def currencies = promotion.map(countries intersect _.appliesTo.countries).getOrElse(countries).flatMap { country => currencyFor(country) }

    def isSixWeek(catalogPlan: CatalogPlan.Paid):Boolean = catalogPlan.charges.billingPeriod match{
      case SixWeeks => true
      case _ => false
    }
    val displaySixForSix: Boolean = promotion.map(promo =>
      catalogPlans.filter(plan => promo.appliesTo.productRatePlanIds.contains(plan.id)).exists(isSixWeek)).getOrElse(false)


    val plans: List[CatalogPlan.Paid] = {if(displaySixForSix)catalogPlans else catalogPlans.filterNot(isSixWeek)}.filter(_.charges.billingPeriod match {case OneYear => false
    case _ => true}).toList.sortBy(_.charges.gbpPrice.amount)

    val discountedPlans = for {
      plan <- plans
      currency <- plan.charges.currencies.toList
      if (currencies.contains(currency))
    } yield {
      val sixOrDiscount = isSixWeek(plan) || promotion.map(_.appliesTo.productRatePlanIds.contains(plan.id)).getOrElse(false)
      val pretty = promotion.filter(_.appliesTo.productRatePlanIds.contains(plan.id)).flatMap(_.asDiscount)
        .map { discountPromo => plan.charges.prettyPricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency) }
        .getOrElse(plan.charges.billingPeriod match {
          case SixWeeks => s"${currency.identifier}6 for six issues"
          case _ => plan.charges.prettyPricing(currency)
        })
      val headline = promotion.flatMap(_.asDiscount)
        .map { discountPromo => plan.charges.headlinePricingForDiscountedPeriod[scalaz.Id.Id, WeeklyLandingPage](discountPromo, currency) }.getOrElse(plan.charges.billingPeriod match {
        case SixWeeks => s"${currency.identifier}6 for six issues"
        case _ => plan.charges.prettyPricing(currency)
      })
      val checkout = s"checkout/${plan.slug}" ? ("countryGroup" -> CountryGroup.allGroups.find(_.currency == currency).getOrElse(CountryGroup.UK).id)
      val url = promoCode.map(p => checkout & ("promoCode" -> p.get)).getOrElse(checkout)
      DiscountedPlan(
        currency = currency,
        pretty = pretty,
        headline = headline,
        period = plan.charges.billingPeriod.adjective.capitalize,
        url = url,
        discounted = sixOrDiscount)
    }
    val p = discountedPlans.partition(_.currency==Currency.USD)
    p._1 ++ p._2
  }

}
