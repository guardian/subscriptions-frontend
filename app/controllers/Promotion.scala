package controllers


import actions.CommonActions._
import com.gu.i18n._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.promo.Promotion.{AnyPromotion, _}
import com.gu.memsub.subsv2.PaidChargeList
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._
import play.api.mvc._
import services._
import utils.TestUsers.PreSigninTestCookie
import views.html.{checkout => view}
import views.support.Pricing._
import views.support.{BillingPeriod => _}


object Promotion extends Controller with LazyLogging with CatalogProvider {

  def getAdjustedRatePlans(promo: AnyPromotion, country:Country, requestedCurrency: Option[Currency])(implicit tpBackend:TouchpointBackend): Option[Map[String, String]] = {
    def impliedCurrency = CountryGroup.byCountryCode(country.alpha2).getOrElse(CountryGroup.UK).currency
    case class RatePlanPrice(ratePlanId: ProductRatePlanId, chargeList: PaidChargeList)
    promo.asDiscount.map { discountPromo =>
      catalog.allSubs.flatten
        .filter(plan => promo.appliesTo.productRatePlanIds.contains(plan.id))
        .map(plan => RatePlanPrice(plan.id, plan.charges)).map { ratePlanPrice =>
        val currency = requestedCurrency.getOrElse(impliedCurrency)
        ratePlanPrice.ratePlanId.get -> ratePlanPrice.chargeList.prettyPricingForDiscountedPeriod(discountPromo, currency)
      }.toMap
    }
  }


  def validateForProductRatePlan(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country, currency: Option[Currency]) = NoCacheAction { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    tpBackend.promoService.findPromotion(promoCode).fold {
      NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code."))
    } { promo =>
      val result = promo.validateFor(prpId, country)
      val body = Json.obj(
        "promotion" -> Json.toJson(promo),
        "adjustedRatePlans" -> Json.toJson(getAdjustedRatePlans(promo, country, currency)),
        "isValid" -> result.isRight,
        "errorMessage" -> result.swap.toOption.map(_.msg)
      )
      result.fold(_ => NotAcceptable(body), _ => Ok(body))
    }
  }

  def validate(promoCode: PromoCode, country: Country, currency: Option[Currency]) = NoCacheAction { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    tpBackend.promoService.findPromotion(promoCode).fold {
      NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code."))
    } { promo =>
      val result = promo.validate(country)
      val body = Json.obj(
        "promotion" -> Json.toJson(promo),
        "adjustedRatePlans" -> Json.toJson(getAdjustedRatePlans(promo, country, currency)),
        "isValid" -> result.isRight,
        "errorMessage" -> result.swap.toOption.map(_.msg)
      )
      result.fold(_ => NotAcceptable/*should be 404*/(body), _ => Ok(body))
    }
  }


}
