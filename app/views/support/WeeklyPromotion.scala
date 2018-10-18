package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.promo.CovariantIdObject.CovariantId
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}
import com.gu.memsub.subsv2.{CatalogPlan, WeeklyPlans}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.GuardianWeeklyZones
import model.PurchasableWeeklyProducts._
import services.WeeklyPicker
import views.support.Pricing._

object WeeklyPromotion {
  private val UK = CountryGroup.UK.countries.toSet
  private val UKdomestic = Set(CountryGroup.C.UK)
  private val UKoverseas = UK diff UKdomestic

  private val allCountries = CountryGroup.allGroups.flatMap(_.countries).toSet

  case class DiscountedPlan(currency: Currency, pretty: String, headline: String, period: String, url: Uri, discounted: Boolean)

  case class DiscountedRegion(title: String, description: String, discountedPlans: List[DiscountedPlan])

  def domesticRegions(promotion: Option[PromoWithWeeklyLandingPage],
                               promoCode: Option[PromoCode])(implicit weeklyPlans: WeeklyPlans): Map[CountryGroup, DiscountedRegion] = {

    val promotionCountries = promotion.map(_.appliesTo.countries).getOrElse(allCountries)

    val UKregion: DiscountedRegion = {

      val productForUK = WeeklyPicker.product(Country.UK)

      val all = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes Isle of Man and Channel Islands",
        discountedPlans = plansForPromotion(promotion, promoCode, productForUK, Currency.GBP)
      )
      val domestic = DiscountedRegion(
        title = "United Kingdom",
        description = "Includes mainland UK only.",
        discountedPlans = plansForPromotion(promotion, promoCode, productForUK, Currency.GBP)
      )
      val overseas = DiscountedRegion(
        title = "Isle of Man and Channel Islands",
        description = "",
        discountedPlans = plansForPromotion(promotion, promoCode, productForUK, Currency.GBP)
      )
      val countries = promotionCountries intersect UK
      val includesUKDomestic = (countries intersect UKdomestic).nonEmpty
      val includesUKOverseas = (countries intersect UKoverseas).nonEmpty

      if(includesUKDomestic && includesUKOverseas){
        all
      } else if (includesUKDomestic) {
        domestic
      } else {
        overseas
      }
    }
    val USregion = DiscountedRegion(
      title = "United States",
      description = "Includes Alaska and Hawaii",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.US), Currency.USD)
    )
    val AUSregion = DiscountedRegion(
      title = "Australia",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.Australia), Currency.AUD)
    )
    val NZregion = DiscountedRegion(
      title = "New Zealand",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.NewZealand), Currency.NZD)
    )
    val CAregion = DiscountedRegion(
      title = "Canada",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.Canada), Currency.CAD)
    )
    val EUregion = DiscountedRegion(
      title = "Europe",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.productForCountryGroup(CountryGroup.Europe), Currency.EUR)
    )

    Map(
      CountryGroup.UK -> UKregion,
      CountryGroup.US -> USregion,
      CountryGroup.Europe -> EUregion,
      CountryGroup.Australia -> AUSregion,
      CountryGroup.NewZealand -> NZregion,
      CountryGroup.Canada -> CAregion
    )
  }

  //If the request country belongs to a domestic country group, we display that country at the top of the list
  //However, if it corresponds to a region that is always displayed then we need to ensure we don't show it twice
  //e.g. if the request country is the US, then it is the prioritised country and we need to not show it again in the
  //list of domestic countries. A rest of world country should always be at the top, and then all the domestic country groups after.
  def domesticCountryGroupsToDisplay(requestCountry: Country): List[CountryGroup] = {
    val dedupedRegions = requestCountry match {
      case Country.UK => GuardianWeeklyZones.domesticZoneCountryGroups.filterNot(_ == CountryGroup.UK)
      case Country.US => GuardianWeeklyZones.domesticZoneCountryGroups.filterNot(_ == CountryGroup.US)
      case Country.Australia => GuardianWeeklyZones.domesticZoneCountryGroups.filterNot(_ == CountryGroup.Australia)
      case Country.NewZealand => GuardianWeeklyZones.domesticZoneCountryGroups.filterNot(_ == CountryGroup.NewZealand)
      case Country.Canada => GuardianWeeklyZones.domesticZoneCountryGroups.filterNot(_ == CountryGroup.Canada)
      case _ => GuardianWeeklyZones.domesticZoneCountryGroups
    }

    dedupedRegions.toList
  }

  def validRegionsForPromotion(promotion: Option[PromoWithWeeklyLandingPage],
                               promoCode: Option[PromoCode],
                               requestCountry: Country)(implicit weeklyPlans: WeeklyPlans): Seq[DiscountedRegion] = {

    val domesticDiscountedRegions: Map[CountryGroup, DiscountedRegion] = domesticRegions(promotion, promoCode)

    // If there is a request country passed by query string, we want it to be at the top.
    // If the request country is a domestic country then it is still the prioritised country, but we should not duplicate it
    val prioritisedRegion: DiscountedRegion = {
      val currency = CountryGroup.byCountryCode(requestCountry.alpha2).map(_.currency).getOrElse(Currency.USD)

      val maybeDomesticDiscountedRegion = for {
        domesticCountryGroup <- GuardianWeeklyZones.getDomesticCountryGroup(requestCountry)
        domesticDiscountedRegion <- domesticDiscountedRegions.get(domesticCountryGroup)
      } yield {
        domesticDiscountedRegion
      }

      maybeDomesticDiscountedRegion.getOrElse(
        DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(requestCountry), currency)
        )
      )
    }

    val restOfWorldRegion = DiscountedRegion(
    title = "Rest of the world",
    description = "Posted to you by air mail",
    discountedPlans = plansForPromotion(promotion, promoCode, WeeklyRestOfWorld, Currency.USD)
    )


    val domesticDiscountedRegionsDeduped = domesticCountryGroupsToDisplay(requestCountry) map { countryGroup =>
      domesticDiscountedRegions(countryGroup)
    }

    val regions: Seq[DiscountedRegion] = prioritisedRegion :: domesticDiscountedRegionsDeduped ++ Seq(restOfWorldRegion)
    regions.filter(_.discountedPlans.nonEmpty)
  }

  def plansForPromotion(
                         maybePromotion: Option[PromoWithWeeklyLandingPage],
                         promoCode: Option[PromoCode],
                         availableWeeklyProduct: PurchasableWeeklyProduct,
                         currency: Currency
  )(implicit allWeeklyPlans: WeeklyPlans): List[DiscountedPlan] = {

    val catalogPlans = plansForProduct(availableWeeklyProduct)

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



