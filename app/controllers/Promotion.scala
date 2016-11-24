package controllers


import actions.CommonActions._
import com.gu.i18n._
import com.gu.identity.play.ProxiedIP
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Promotion.{AnyPromotion, _}
import com.gu.memsub.promo.{NewUsers, PromoCode}
import com.gu.memsub.subsv2.{CatalogPlan, PaidChargeList}
import com.gu.memsub.{Product, SupplierCode}
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.IdUserOps._
import model._
import model.error.CheckoutService._
import model.error.SubsError
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import services.AuthenticationService.authenticatedUserFor
import services._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{PlanList, BillingPeriod => _, _}

import scala.Function.const
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.std.option._
import scalaz.std.scalaFuture._
import scalaz.syntax.applicative._
import scalaz.{NonEmptyList, OptionT}
import model.ContentSubscriptionPlanOps._

object Promotion extends Controller with LazyLogging with CatalogProvider {
  def validatePromoCode(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country) = NoCacheAction { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    import com.gu.memsub.Subscription.ProductRatePlanId
    import views.support.Pricing._

    def getAdjustedRatePlans(promo: AnyPromotion): Option[Map[String, String]] = {
      case class RatePlanPrice(ratePlanId: ProductRatePlanId, chargeList: PaidChargeList)
      promo.asDiscount.map { discountPromo =>
        catalog.allSubs.flatten
          .filter(plan => promo.appliesTo.productRatePlanIds.contains(plan.id))
          .map(plan => RatePlanPrice(plan.id, plan.charges)).map { ratePlanPrice =>
          val currency = CountryGroup.byCountryCode(country.alpha2).getOrElse(CountryGroup.UK).currency
          ratePlanPrice.ratePlanId.get -> ratePlanPrice.chargeList.prettyPricingForDiscountedPeriod(discountPromo, currency)
        }.toMap
      }
    }

    tpBackend.promoService.findPromotion(promoCode).fold {
      NotFound(Json.obj("errorMessage" -> s"Sorry, we can't find that code."))
    } { promo =>
      val result = promo.validateFor(prpId, country)
      val body = Json.obj(
        "promotion" -> Json.toJson(promo),
        "adjustedRatePlans" -> Json.toJson(getAdjustedRatePlans(promo)),
        "isValid" -> result.isRight,
        "errorMessage" -> result.swap.toOption.map(_.msg)
      )
      result.fold(_ => NotAcceptable(body), _ => Ok(body))
    }
  }

}
