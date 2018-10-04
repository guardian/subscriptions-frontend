package views.support

import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.images.{ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.promo.CovariantIdObject.CovariantId
import com.gu.memsub.promo.Promotion.PromoWithWeeklyLandingPage
import com.gu.memsub.promo.{PromoCode, WeeklyLandingPage}
import com.gu.memsub.subsv2.{CatalogPlan, WeeklyPlans}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.GuardianWeeklyZones
import model.PurchasableWeeklyProducts._
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
                               rawQueryString: String = "")(implicit weeklyPlans: WeeklyPlans): Seq[DiscountedRegion] = {

    val promotionCountries = promotion.map(_.appliesTo.countries).getOrElse(allCountries)
    val newPricing = WeeklyPicker.newPricingQueryStringPresent(rawQueryString)

    // If a user does not qualify for domestic delivery (e.g. if the user is based in South Africa),
    // we want to explicitly call out their (likely) delivery country on the landing page
    val promotedRegion: Seq[DiscountedRegion] = {
      if (WeeklyPicker.isInRestOfWorldOrZoneC(requestCountry: Country, WeeklyPicker.showUpdatedPrices(newPricing))) {
        val currency = CountryGroup.byCountryCode(requestCountry.alpha2).map(_.currency).getOrElse(Currency.USD)
        Seq(DiscountedRegion(
          title = requestCountry.name,
          description = "Posted to you by air mail",
          discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(requestCountry, WeeklyPicker.showUpdatedPrices(newPricing)), currency)
        ))
      } else {
        Seq()
      }
    }

    val restOfWorldRegion = Seq(DiscountedRegion(
      title = "Rest of the world",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.restOfWorldOrZoneC(WeeklyPicker.showUpdatedPrices(newPricing)), Currency.USD)
    ))

    val UKregion: Set[DiscountedRegion] = {

      val productForUK = WeeklyPicker.product(Country.UK, WeeklyPicker.showUpdatedPrices(newPricing))

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
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.US, WeeklyPicker.showUpdatedPrices(newPricing)), Currency.USD)
    ))
    val AUSregion =  if (CountryGroup.Australia.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Australia",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.Australia, WeeklyPicker.showUpdatedPrices(newPricing)), Currency.AUD)
    ))
    val NZregion =  if (CountryGroup.NewZealand.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "New Zealand",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.NewZealand, WeeklyPicker.showUpdatedPrices(newPricing)), Currency.NZD)
    ))
    val CAregion =  if (CountryGroup.Canada.countries contains requestCountry) Seq() else Seq(DiscountedRegion(
      title = "Canada",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.product(Country.Canada, WeeklyPicker.showUpdatedPrices(newPricing)), Currency.CAD)
    ))
    val EUregion = Seq(DiscountedRegion(
      title = "Europe",
      description = "Posted to you by air mail",
      discountedPlans = plansForPromotion(promotion, promoCode, WeeklyPicker.productForCountryGroup(CountryGroup.Europe, WeeklyPicker.showUpdatedPrices(newPricing)), Currency.EUR)
    ))

    val regions: Seq[DiscountedRegion] = promotedRegion ++ UKregion ++ USregion ++ EUregion ++ AUSregion ++  NZregion ++ CAregion  ++ restOfWorldRegion
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

object WeeklyPicker {

  import model.GuardianWeeklyZones._

  val updateTime = DateTime.parse("2018-10-10T09:45:00").withZone(DateTimeZone.UTC)

  def newPricingQueryStringPresent(rawQueryString: String) = rawQueryString.contains("gwoct18")

  def showUpdatedPrices(forceShowUpdatedPrices: Boolean, timeToUpdate: DateTime = updateTime): Boolean = {
    val now = DateTime.now().withZone(DateTimeZone.UTC)
    now.isAfter(timeToUpdate) || forceShowUpdatedPrices
  }

  def isInRestOfWorldOrZoneC(country: Country, showUpdatedPrices: Boolean): Boolean = {
    if (showUpdatedPrices) GuardianWeeklyZones.restOfWorldZoneCountries.contains(country)
    else GuardianWeeklyZones.zoneCCountries.contains(country)
  }

  def restOfWorldOrZoneC(showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) WeeklyRestOfWorld
    else WeeklyZoneC
  }

  def product(country: Country, showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) {
      if (domesticZoneCountries.contains(country)) WeeklyDomestic
      else WeeklyRestOfWorld
    }
    else {
      if (zoneACountries.contains(country)) WeeklyZoneA
      else WeeklyZoneC
    }
  }

  def productForCountryGroup(countryGroup: CountryGroup, showUpdatedPrices: Boolean): PurchasableWeeklyProduct = {
    if (showUpdatedPrices) {
      if (domesticZoneCountryGroups.contains(countryGroup)) WeeklyDomestic
      else WeeklyRestOfWorld
    }
    else {
      if (zoneACountryGroups.contains(countryGroup)) WeeklyZoneA
      else WeeklyZoneC
    }
  }

}

object ImagePicker {

  private val guardianWeeklyHeaderId = "c7c76ffe9b2abe16b5d914dd7a9a23db9b32840b/0_0_14400_1680"
  private val guardianWeeklyRedesignHeaderId = "c933375535e24a9fd3c2befac96a5fafaaed6f4f/0_0_9985_1165"

  private val guardianWeeklyPackshotId = "987daf55251faf1637f92bffa8aa1eeec8de72b5/0_0_1670_1558"
  private val guardianWeeklyRedesignPackshotId = "4e2eaacb68f29b9573c015c248134dc1614d0fa3/0_0_2155_2800"

  def defaultHeaderImage(rawQueryString: String = ""): ResponsiveImageGroup = {
    WeeklyPicker.showUpdatedPrices(WeeklyPicker.newPricingQueryStringPresent(rawQueryString)) match {
      case true => ResponsiveImageGroup(None,None,None,ResponsiveImageGenerator(guardianWeeklyRedesignHeaderId,Seq(9985), "png"))
      case false => ResponsiveImageGroup(None,None,None,ResponsiveImageGenerator(guardianWeeklyHeaderId,Seq(2000), "jpg"))
    }

  }

  def defaultPackshotImage(rawQueryString: String = ""): ResponsiveImageGroup = {
    WeeklyPicker.showUpdatedPrices(WeeklyPicker.newPricingQueryStringPresent(rawQueryString)) match {
      case true => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyRedesignPackshotId, Seq(385), "png"),
        altText = Some("Stack of The Guardian Weekly editions")
      )
      case false => ResponsiveImageGroup(
        availableImages = ResponsiveImageGenerator(guardianWeeklyPackshotId, Seq(500, 1000), "png"),
        altText = Some("Stack of The Guardian Weekly editions")
      )
    }

  }

}
