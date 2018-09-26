package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.promo.Promotion.{CovariantId, PromoWithWeeklyLandingPage}
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}
import com.gu.memsub.subsv2.{CatalogPlan, Catalog => SubsCatalog}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support.Pricing._

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
  private val ZONEC = allCountries.diff(UK union US)

  case class DiscountedPlan(currency: Currency, pretty: String, headline: String, period: String, url: Uri, discounted: Boolean)

  case class DiscountedRegion(title: String, description: String, countries: Set[Country], discountedPlans: List[DiscountedPlan])

  def validRegionsForPromotion(promotion: Option[PromoWithWeeklyLandingPage], promoCode: Option[PromoCode], requestCountry: Country)(implicit catalog: SubsCatalog): Seq[DiscountedRegion] = {
    val promotionCountries = promotion.map(_.appliesTo.countries).getOrElse(allCountries)

    val regionForZoneCCountry: Seq[DiscountedRegion] = { //todo: a zone C check no longer makes sense in the domestic/row brave new world

      if (ZONEC.contains(requestCountry)) {
        val currency = CountryGroup.byCountryCode(requestCountry.alpha2).map(_.currency).getOrElse(Currency.USD)
        Seq(DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          countries = Set(requestCountry),
          discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(requestCountry),currency)
        ))
      } else {
        Seq()
      }
    }
    val rowWithoutCountry: Seq[DiscountedRegion] = Seq(DiscountedRegion(
        title = "Rest of the world",
        description = "Posted to you by air mail",
        countries = ZONEC - requestCountry,
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.restOfWorldPlans, Currency.USD)
      ))

    val UKregion: Set[DiscountedRegion] = {
      val all = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes Isle of Man and Channel Islands",
        countries = UK,
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK),Currency.GBP)
      )
      val domestic = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes mainland UK only.",
        countries = UKdomestic,
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK),Currency.GBP)
      )
      val overseas = DiscountedRegion(
        title = "Isle of Man and Channel Islands",
        description = "",
        countries = UKoverseas,
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK),Currency.GBP)
      )
      val countries = promotionCountries intersect UK
      val includesUKDomestic = (countries intersect UKdomestic).nonEmpty
      val includesUKOverseas = (countries intersect UKoverseas).nonEmpty
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
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.US),Currency.USD)
    ))
    val AUSregion =  if (AU contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Australia",
      description = "Posted to you by air mail",
      countries = AU,
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.Australia),Currency.AUD)
    ))
    val NZregion =  if (NZ contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "New Zealand",
      description = "Posted to you by air mail",
      countries = NZ,
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.NewZealand),Currency.NZD)
    ))
    val CAregion =  if (CA contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Canada",
      description = "Posted to you by air mail",
      countries = CA,
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.Canada),Currency.CAD)
    ))
    val EUregion = Seq(DiscountedRegion(
      title = "Europe",
      description = "Posted to you by air mail",
      countries = EU,
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.Ireland),Currency.EUR) //todo Europe

    ))
    val regions: Seq[DiscountedRegion] = regionForZoneCCountry ++ UKregion ++ USregion ++ EUregion ++ AUSregion ++  NZregion ++ CAregion  ++ rowWithoutCountry
    regions.filter(_.discountedPlans.nonEmpty)
  }

  def plansForPromotion(maybePromotion: Option[PromoWithWeeklyLandingPage], promoCode: Option[PromoCode], catalogPlans: Seq[CatalogPlan.Paid], currency: Currency): List[DiscountedPlan] = {

    def isSixWeek(catalogPlan: CatalogPlan.Paid): Boolean =
      catalogPlan.charges.billingPeriod match {
        case SixWeeks => true
        case _ => false
      }
    val displaySixForSix: Boolean = maybePromotion.exists(promo =>
      catalogPlans.filter { plan =>
        promo.appliesTo.productRatePlanIds.contains(plan.id)
      }.exists(isSixWeek))

    val plans: List[CatalogPlan.Paid] = {
      if (displaySixForSix) catalogPlans
      else catalogPlans.filterNot(isSixWeek)
    }.filter(_.charges.billingPeriod match {
      case OneYear => false
      case _ => true
    }).toList.sortBy(_.charges.gbpPrice.amount)

    for {
      plan <- plans
      sixOrDiscount = isSixWeek(plan) || maybePromotion.exists(_.appliesTo.productRatePlanIds.contains(plan.id))
      pretty = maybePromotion.filter(_.appliesTo.productRatePlanIds.contains(plan.id)).flatMap(_.asDiscount)
        .map { discountPromo => plan.charges.prettyPricingForDiscountedPeriod[CovariantId, WeeklyLandingPage](discountPromo, currency) }
        .getOrElse(plan.charges.billingPeriod match {
          case SixWeeks => s"${currency.identifier}6 for six issues"
          case _ => plan.charges.prettyPricing(currency)
        })
      headline = maybePromotion.flatMap(_.asDiscount)
        .map { discountPromo => plan.charges.headlinePricingForDiscountedPeriod[CovariantId, WeeklyLandingPage](discountPromo, currency) }.getOrElse(plan.charges.billingPeriod match {
        case SixWeeks => s"${currency.identifier}6 for six issues"
        case _ => plan.charges.prettyPricing(currency)
      })

    } yield {
      DiscountedPlan(
        currency = currency,
        pretty = pretty,
        headline = headline,
        period = plan.charges.billingPeriod.adjective.capitalize,
        url = getCheckoutUrl(maybePromotion, plan, promoCode, currency),
        discounted = sixOrDiscount)
    }
  }

  def getCheckoutUrl(maybePromotion: Option[PromoWithWeeklyLandingPage], plan: CatalogPlan.Paid, promoCode: Option[PromoCode], currency: Currency): Uri = {

    val baseUrl = s"checkout/${plan.slug}" ? ("countryGroup" -> CountryGroup.allGroups.find(_.currency == currency).getOrElse(CountryGroup.UK).id)
    val promotionValidForPlan = maybePromotion.exists(_.appliesTo.productRatePlanIds.contains(plan.id))

    if (promotionValidForPlan && promoCode.isDefined)
      baseUrl & ("promoCode" -> promoCode.get.get)
    else
      baseUrl
  }

}

object PlanPicker {

  private val UK = CountryGroup.UK.countries.toSet
  private val US = CountryGroup.US.countries.toSet
  private val AU = CountryGroup.Australia.countries.toSet
  private val NZ = CountryGroup.NewZealand.countries.toSet
  private val EU = CountryGroup.Europe.countries.toSet
  private val CA = CountryGroup.Canada.countries.toSet

  private val ZONEA = UK ++ US

  private val domesticCountries = Set(UK, US, CA, AU, NZ, EU).flatten //todo is that the definition?

  val showUpdatedPrices = true

  def restOfWorldPlans(implicit catalog: SubsCatalog): Seq[CatalogPlan.Paid]={
    if(showUpdatedPrices) catalog.weekly.restOfWorld.plans
    else catalog.weekly.zoneC.plans
  }
  def plans(country: Country)(implicit catalog: SubsCatalog): Seq[CatalogPlan.Paid] = {
    if(showUpdatedPrices) {
      if(domesticCountries.contains(country)) catalog.weekly.domestic.plans
      else catalog.weekly.restOfWorld.plans
    }
    else {
      if(ZONEA.contains(country)) catalog.weekly.zoneA.plans
      else catalog.weekly.zoneC.plans
    }
  }

}
