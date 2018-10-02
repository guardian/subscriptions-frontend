package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.promo.CovariantIdObject.CovariantId
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}
import com.gu.memsub.subsv2.{CatalogPlan, Catalog => SubsCatalog}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.GuardianWeeklyZones
import views.support.Pricing._
import org.joda.time.{DateTime, DateTimeZone}

object WeeklyPromotion {
  private val UK = CountryGroup.UK.countries.toSet
  private val UKdomestic = Set(CountryGroup.C.UK)
  private val UKoverseas = UK diff UKdomestic

  private val allCountries = CountryGroup.allGroups.flatMap(_.countries).toSet

  case class DiscountedPlan(currency: Currency, pretty: String, headline: String, period: String, url: Uri, discounted: Boolean)

  case class DiscountedRegion(title: String, description: String, discountedPlans: List[DiscountedPlan])

  def validRegionsForPromotion(promotion: Option[PromoWithWeeklyLandingPage],
                               promoCode: Option[PromoCode],
                               requestCountry: Country,
                               rawQueryString: String = "")(implicit catalog: SubsCatalog): Seq[DiscountedRegion] = {
    val promotionCountries = promotion.map(_.appliesTo.countries).getOrElse(allCountries)
    val newPricing = rawQueryString.contains("gwoct18")
    // If a user does not qualify for domestic delivery (e.g. if the user is based in South Africa),
    // we want to explicitly call out their (likely) delivery country on the landing page
    val promotedRegion: Seq[DiscountedRegion] = {
      if (PlanPicker.isInRestOfWorldOrZoneC(requestCountry: Country, forceShowUpdatedPrices = newPricing)) {
        val currency = CountryGroup.byCountryCode(requestCountry.alpha2).map(_.currency).getOrElse(Currency.USD)
        Seq(DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(requestCountry, forceShowUpdatedPrices = newPricing),currency)
        ))
      } else {
        Seq()
      }
    }

    val restOfWorldRegion = Seq(DiscountedRegion(
      title = "Rest of the world",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.restOfWorldOrZoneCPlans(forceShowUpdatedPrices = newPricing), Currency.USD)
    ))

    val UKregion: Set[DiscountedRegion] = {
      val all = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes Isle of Man and Channel Islands",
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK, forceShowUpdatedPrices = newPricing),Currency.GBP)
      )
      val domestic = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes mainland UK only.",
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK, forceShowUpdatedPrices = newPricing),Currency.GBP)
      )
      val overseas = DiscountedRegion(
        title = "Isle of Man and Channel Islands",
        description = "",
        discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.UK, forceShowUpdatedPrices = newPricing),Currency.GBP)
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
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.US, forceShowUpdatedPrices = newPricing),Currency.USD)
    ))
    val AUSregion =  if (CountryGroup.Australia.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Australia",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.Australia, forceShowUpdatedPrices = newPricing),Currency.AUD)
    ))
    val NZregion =  if (CountryGroup.NewZealand.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "New Zealand",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.NewZealand, forceShowUpdatedPrices = newPricing),Currency.NZD)
    ))
    val CAregion =  if (CountryGroup.Canada.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Canada",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plans(Country.Canada, forceShowUpdatedPrices = newPricing),Currency.CAD)
    ))
    val EUregion = Seq(DiscountedRegion(
      title = "Europe",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, PlanPicker.plansForCountryGroup(CountryGroup.Europe, forceShowUpdatedPrices = newPricing),Currency.EUR)
    ))

    val regions: Seq[DiscountedRegion] = promotedRegion ++ UKregion ++ USregion ++ EUregion ++ AUSregion ++  NZregion ++ CAregion  ++ restOfWorldRegion
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

  import model.GuardianWeeklyZones._

  def showUpdatedPrices = {
    val now = DateTime.now().withZone(DateTimeZone.UTC)
    val threshold = DateTime.parse("2018-10-10T08:00:00").withZone(DateTimeZone.UTC)
    now.isAfter(threshold)
  }

  def isInRestOfWorldOrZoneC(country: Country, forceShowUpdatedPrices: Boolean = false) = {
    if(showUpdatedPrices || forceShowUpdatedPrices) GuardianWeeklyZones.restOfWorldZoneCountries.contains(country)
    else GuardianWeeklyZones.zoneCCountries.contains(country)
  }

  def restOfWorldOrZoneCPlans(forceShowUpdatedPrices: Boolean = false)(implicit catalog: SubsCatalog): Seq[CatalogPlan.Paid] = {
    if(showUpdatedPrices || forceShowUpdatedPrices) catalog.weekly.restOfWorld.plans
    else catalog.weekly.zoneC.plans
  }

  def plans(country: Country, forceShowUpdatedPrices: Boolean = false)(implicit catalog: SubsCatalog): Seq[CatalogPlan.Paid] = {
    if(showUpdatedPrices || forceShowUpdatedPrices) {
      if(domesticZoneCountries.contains(country)) catalog.weekly.domestic.plans
      else catalog.weekly.restOfWorld.plans
    }
    else {
      if(zoneACountries.contains(country)) catalog.weekly.zoneA.plans
      else catalog.weekly.zoneC.plans
    }
  }

  def plansForCountryGroup(countryGroup: CountryGroup, forceShowUpdatedPrices: Boolean = false)(implicit catalog: SubsCatalog): Seq[CatalogPlan.Paid] = {
    if(showUpdatedPrices || forceShowUpdatedPrices) {
      if(domesticZoneCountryGroups.contains(countryGroup)) catalog.weekly.domestic.plans
      else catalog.weekly.restOfWorld.plans
    }
    else {
      if(zoneACountryGroups.contains(countryGroup)) catalog.weekly.zoneA.plans
      else catalog.weekly.zoneC.plans
    }
  }

}
