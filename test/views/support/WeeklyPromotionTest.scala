//package views.support
//
//import java.util.UUID
//
//import com.gu.i18n.{Country, CountryGroup}
//import com.gu.i18n.Currency.{CAD, EUR, NZD}
//import com.gu.memsub.Benefit.Weekly
//import com.gu.memsub.BillingPeriod.Quarter
//import com.gu.memsub.Product.WeeklyZoneC
//import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId, SubscriptionRatePlanChargeId}
//import com.gu.memsub.promo._
//import com.gu.memsub.subsv2._
//import org.specs2.mock.Mockito
//import org.specs2.mutable.Specification
//import com.gu.memsub._
//import com.gu.memsub.promo.Promotion.CovariantId
//
//class WeeklyPromotionTest extends Specification with Mockito {
//
//  "plansForPromotion" should {
//    "give plans for promotion, existing" in {
//      //mock the promotion??
//
//      val pricingSummary = PricingSummary(Map(CAD -> Price(999.0f,CAD), NZD -> Price(98.0f,NZD), EUR -> Price(49.0f,EUR)))
//      val productRatePlanChargeId = ProductRatePlanChargeId("productRatePlanChargeId")
//      val productRatePlanId = com.gu.memsub.Subscription.ProductRatePlanId("abc")
//      val paidCharge = PaidCharge(Weekly, Quarter, pricingSummary,productRatePlanChargeId,SubscriptionRatePlanChargeId("abc"))
//      val plan = CatalogPlan(
//        ProductRatePlanId("2c92c0f858aa38af0158da325cec0b2e"),
//        WeeklyZoneC,
//        "Guardian Weekly Quarterly",
//        "",
//        None,
//        paidCharge,
//        Current()
//      )
//
//      val mockPromotion = mock[Promotion.PromoWithWeeklyLandingPage]
//      val promotion: Promotion[PromotionType[PromoContext], CovariantId, WeeklyLandingPage] = Promotion(
//        uuid = UUID.randomUUID(),
//        name = "Guardian weekly 10% off",
//        description = "",
//        appliesTo = AppliesTo(Set(productRatePlanId), CountryGroup.NewZealand.countries.toSet),
//        campaign = CampaignCode("campaignCode"),
//        channelCodes = Map(),
//        landingPage = None,
//        starts = Imports.DateTime,
//        expires = expires,
//        promotionType = promotionType
//      )
//
//      mockPromotion.appliesTo.productRatePlanIds.contains(productRatePlanId) returns true
//
//      val plans = WeeklyPromotion.plansForPromotion(Some(mockPromotion), Some(PromoCode("ANNUAL10")), List(plan), NZD)
//
//      plans shouldEqual("HAHAHAHAHAHA")
//    }
//
//  }
//
//}
