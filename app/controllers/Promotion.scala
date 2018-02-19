package controllers


import actions.CommonActions
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

import scala.concurrent.{ExecutionContext, Future}

class Promotion(fBackendFactory: TouchpointBackends, commonActions: CommonActions)(implicit val executionContext: ExecutionContext) extends Controller with LazyLogging {

  import commonActions._

  private val fallbackCurrency = CountryGroup.UK.currency

  private def getDefaultCurrencyForCountry(country: Country) = CountryGroup.byCountryCode(country.alpha2).map(_.currency)

  def getAdjustedRatePlans(promo: AnyPromotion, country: Country, requestedCurrency: Option[Currency])(implicit tpBackend: TouchpointBackend): Future[Option[Map[String, String]]] = {
    val currency = requestedCurrency orElse getDefaultCurrencyForCountry(country) getOrElse fallbackCurrency

    case class RatePlanPrice(ratePlanId: ProductRatePlanId, chargeList: PaidChargeList)
    promo.asDiscount.map { discountPromo =>
      tpBackend.catalogService.catalog.map(_.map { catalog =>
        catalog.allSubs.flatten
          .filter(plan => promo.appliesTo.productRatePlanIds contains plan.id)
          .filter(plan => plan.charges.currencies contains currency)
          .map(plan => RatePlanPrice(plan.id, plan.charges)).map { ratePlanPrice =>
          ratePlanPrice.ratePlanId.get -> ratePlanPrice.chargeList.prettyPricingForDiscountedPeriod(discountPromo, currency)
        }.toMap
      }.toOption
      )
    }.getOrElse(Future.successful(None))
  }


  def validateForProductRatePlan(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country, currency: Option[Currency]) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    tpBackend.promoService.findPromotionFuture(promoCode).flatMap { promotion =>
      promotion.filterNot(_.isTracking).fold {
        Future.successful(NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code.")))
      } {
        promo =>
          getAdjustedRatePlans(promo, country, currency).map { ratePlans =>
            val result = promo.validateFor(prpId, country)

            def body = Json.obj(
              "promotion" -> Json.toJson(promo),
              "adjustedRatePlans" -> Json.toJson(ratePlans),
              "isValid" -> result.isRight,
              "errorMessage" -> result.swap.toOption.map(_.msg)
            )

            result.fold(_ => NotAcceptable(body), _ => Ok(body))
          }
      }
    }
  }


  def validate(promoCode: PromoCode, country: Country, currency: Option[Currency]) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    tpBackend.promoService.findPromotionFuture(promoCode).flatMap { promotion =>
      promotion.fold {
        Future.successful(NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code.")))
      } { promo =>
        getAdjustedRatePlans(promo, country, currency).map { ratePlans =>
          val result = promo.validate(country)

          def body = Json.obj(
            "promotion" -> Json.toJson(promo),
            "adjustedRatePlans" -> Json.toJson(ratePlans),
            "isValid" -> result.isRight,
            "errorMessage" -> result.swap.toOption.map(_.msg)
          )

          result.fold(_ => NotAcceptable /*!?! fix this*/ (body), _ => Ok(body))
        }

      }
    }
  }

}
