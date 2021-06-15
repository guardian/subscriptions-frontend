package controllers

import java.util.UUID

import actions.CommonActions
import cats.data.EitherT
import cats.instances.future._
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.i18n.CountryGroup._
import com.gu.i18n.Currency._
import com.gu.i18n._
import com.gu.memsub.Subscription.Name
import com.gu.memsub.promo.Promotion._
import com.gu.memsub.promo.{NormalisedPromoCode, PromoCode}
import com.gu.memsub.subsv2.CatalogPlan.ContentSubscription
import com.gu.memsub.subsv2.{CatalogPlan, PlansWithIntroductory}
import com.gu.memsub.{Product, SupplierCode}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config.Identity.webAppProfileUrl
import forms.{FinishAccountForm, SubscriptionsForm}
import model.ContentSubscriptionPlanOps._
import model.IdUserOps._
import model._
import model.error.CheckoutService._
import model.error.SubsError
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json._
import play.api.mvc._
import scalaz.std.scalaFuture._
import scalaz.{NonEmptyList, OptionT}
import services._
import utils.RequestCountry._
import utils.{PaymentGatewayError, TestUsers}
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import views.html.{checkout => view}
import views.support.{PlanList, BillingPeriod => _, _}

import scala.Function.const
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Checkout(fBackendFactory: TouchpointBackends, authenticationService: AuthenticationService, commonActions: CommonActions, implicit val executionContext: ExecutionContext, override protected val controllerComponents: ControllerComponents) extends BaseController with LazyLogging {

  import SessionKeys.{Currency => _, UserId => _, _}
  import commonActions._

  def checkoutService(implicit res: TouchpointBackends.Resolution): CheckoutService =
    res.backend.checkoutService

  def getBetterPlans[A <: CatalogPlan.Paid](plan: A, allPlans: List[A]) =
    allPlans.sortBy(_.charges.gbpPrice.amount).dropWhile(_.charges.gbpPrice.amount <= plan.charges.gbpPrice.amount)

  def renderCheckout(countryGroup: String, promoCode: Option[PromoCode], supplierCode: Option[SupplierCode], forThisPlan: String) = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend

    // countryGroup String above basically means now 'countryOrCountryGroup' so we'll use the fromHint API on DetermineCountryGroup
    val determinedCountryGroup = DetermineCountryGroup.fromHint(countryGroup) getOrElse UK

    tpBackend.catalogService.catalog.map { _.map { catalog =>

      val matchingPlanList: Option[PlanList[ContentSubscription]] = {

        val productsWithoutIntroductoryPlans = List(
          catalog.delivery.list.toList,
          catalog.voucher.list.toList,
          catalog.digipack.plans
        ).map(plans => PlansWithIntroductory(plans, List.empty))

        val productsWithIntroductoryPlans = List(
          catalog.weekly.domestic.plansWithAssociations,
          catalog.weekly.restOfWorld.plansWithAssociations
        )

        val contentSubscriptionPlans = productsWithoutIntroductoryPlans ++ productsWithIntroductoryPlans
        contentSubscriptionPlans.flatMap {
          case PlansWithIntroductory(plans, associations)
            if (plans.exists(_.slug == forThisPlan)) => {
            val buyablePlans = plans.filter(_.availableForCheckout)
            val plansInPriceOrder = buyablePlans.sortBy(_.charges.gbpPrice.amount)
            val selectedPlan = plansInPriceOrder.find(_.slug == forThisPlan)
            selectedPlan.map(PlanList(associations,_,plansInPriceOrder))
          }
          case _ => None
        }.headOption
      }

      val idUser = (for {
        authUser <- OptionT(Future.successful(authenticationService.authenticatedUserFor(request)))
        idUser <- OptionT(fBackendFactory.identityService.userLookupByCredentials(authUser.credentials))
      } yield idUser).run

      idUser map { user =>

        def renderCheckoutFor(planList: PlanList[CatalogPlan.ContentSubscription]) = {

          val personalData = user.map(PersonalData.fromIdUser)

          def getSettings(fallbackCountry: Option[Country], fallbackCurrency: Currency) = {
            val localizationSettings = planList.default.localizationSettings
            val availableCountries = localizationSettings.availableDeliveryCountriesWithCurrency.getOrElse(localizationSettings.availableBillingCountriesWithCurrency)

            def availableCountryWithCurrencyFor(country: Option[Country]) = country.flatMap(c => availableCountries.find(_.country == c))
            val personalDataCountry = personalData.flatMap(data => data.address.country)

            val defaultCountryWithCurrency = availableCountryWithCurrencyFor(personalDataCountry) orElse availableCountryWithCurrencyFor(fallbackCountry)

            CountryAndCurrencySettings(
              availableDeliveryCountries = localizationSettings.availableDeliveryCountriesWithCurrency,
              availableBillingCountries = localizationSettings.availableBillingCountriesWithCurrency,
              defaultCountry = defaultCountryWithCurrency.map(_.country),
              defaultCurrency = defaultCountryWithCurrency.map(_.currency).getOrElse(fallbackCurrency)
            )
          }

          val countryAndCurrencySettings = planList.default.product match {
            case Product.Delivery => getSettings(UK.defaultCountry, UK.currency)
            case Product.Voucher => getSettings(UK.defaultCountry, UK.currency)
            case Product.WeeklyDomestic => {
              if (GuardianWeeklyZones.domesticZoneCountryGroups.contains(determinedCountryGroup)) {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              } else {
                getSettings(UK.defaultCountry, UK.currency)
              }
            }
            case Product.WeeklyRestOfWorld => {
              if (GuardianWeeklyZones.domesticZoneCountryGroups.contains(determinedCountryGroup)) {
                getSettings(None, USD)
              } else {
                getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
              }
            }
            case _ => getSettings(determinedCountryGroup.defaultCountry, determinedCountryGroup.currency)
          }

          val digitalEdition = model.DigitalEdition.getForCountry(countryAndCurrencySettings.defaultCountry)

          // either a code to send to the form (left) or a tracking code for the session (right)
          val countryToValidatePromotionAgainst = countryAndCurrencySettings.defaultCountry orElse determinedCountryGroup.defaultCountry

          val promotion = promoCode.flatMap(tpBackend.promoService.findPromotion)

          val trackingPromotion = promotion.flatMap(_.asTracking)
          val displayPromoCode = if(promotion.isDefined && trackingPromotion.isEmpty) promoCode else None
          val trackingCodeSessionData: Seq[(String, String)] = trackingPromotion.flatMap(_ => promoCode.map(p => (PromotionTrackingCode -> p.get))).toSeq

          val resolvedSupplierCode = supplierCode orElse request.session.get(SupplierTrackingCode).map(SupplierCode) // query param wins
          val supplierCodeSessionData = resolvedSupplierCode.map(code => Seq(SupplierTrackingCode -> code.get)).getOrElse(Seq.empty)
          val productData = ProductPopulationData(user.map(_.correspondenceAddress), planList)
          Ok(views.html.checkout.payment(
            personalData = personalData,
            productData = productData,
            countryGroup = determinedCountryGroup,
            touchpointBackendResolution = resolution,
            promoCode = displayPromoCode,
            supplierCode = resolvedSupplierCode,
            edition = digitalEdition,
            countryAndCurrencySettings = countryAndCurrencySettings
          )).withSession(request.session.data.toSeq ++ trackingCodeSessionData ++ supplierCodeSessionData: _*)
        }

        matchingPlanList match {
          case Some(planList) => renderCheckoutFor(planList)
          case None => Redirect(routes.Homepage.index())
        }
      }
    }.valueOr { err =>
      SafeLogger.error(scrub"failed renderCheckout: ${err.list.toList.mkString(", ")}")
      Future.successful(InternalServerError("failed to render checkout due to catalog issues"))
    }
    }.flatMap(identity)
  }

  def convertGuestUser = NoCacheAction.async(parse.form(FinishAccountForm())) { implicit request =>
    val guestAccountData = request.body
    val marketingOptIn = Try(request.session.get(MarketingOptIn).getOrElse("false").toBoolean).getOrElse(false)

    fBackendFactory.identityService.convertGuest(guestAccountData.password, IdentityToken(guestAccountData.token), marketingOptIn)
      .map { cookies =>
        Ok(Json.obj("profileUrl" -> webAppProfileUrl.toString())).withCookies(cookies: _*)
      }
  }

  def thankYou() = NoCacheAction.async { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    val session = request.session

    val sessionInfo = for {
      subsName <- session.get(SubsName)
      startDate <- session.get(StartDate)
    } yield (subsName, startDate)
    sessionInfo.fold {
      Future.successful(Redirect(routes.Homepage.index()).withNewSession)
    } { case (subsName, startDate) =>

      val passwordForm = authenticationService.authenticatedUserFor(request).fold {
        for {
          userId <- session.get(SessionKeys.UserId)
          token <- session.get(IdentityGuestPasswordSettingToken)
          form <- GuestUser(UserId(userId), IdentityToken(token)).toGuestAccountForm
        } yield form
      } {
        const(None)
      } // Don't display the user registration form if the user is logged in

      val promoCode = session.get(AppliedPromoCode)
      val promotion = promoCode.flatMap(code => resolution.backend.promoService.findPromotion(NormalisedPromoCode.safeFromString(code)))

      val billingCountry = session.get(BillingCountryName).flatMap(CountryGroup.countryByNameOrCode) orElse request.getFastlyCountry
      val edition: model.DigitalEdition = model.DigitalEdition.getForCountry(billingCountry)
      val eventualMaybeSubscription = tpBackend.subscriptionService.get[com.gu.memsub.subsv2.SubscriptionPlan.ContentSubscription](Name(subsName))
      eventualMaybeSubscription.map { maybeSub =>
        maybeSub.map { sub =>
          Ok(view.thankyou(sub, passwordForm, resolution, promoCode, promotion, startDate, edition, billingCountry))
        }.getOrElse {
          Redirect(routes.Homepage.index()).withNewSession
        }
      }
    }

  }

  def checkIdentity(email: String) = NoCacheAction.async { implicit request =>
    for {
      doesUserExist <- fBackendFactory.identityService.doesUserExist(email)
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  val parseDirectDebitForm: BodyParser[DirectDebitData] = parse.form[DirectDebitData](Form(SubscriptionsForm.directDebitDataMapping))

  def checkAccount = NoCacheAction.async(parseDirectDebitForm) { implicit request =>
    implicit val resolution: TouchpointBackends.Resolution = fBackendFactory.forRequest(PreSigninTestCookie, request.cookies)
    implicit val tpBackend = resolution.backend
    for {
      isAccountValid <- tpBackend.goCardlessService.checkBankDetails(request.body)
    } yield Ok(Json.obj("accountValid" -> isAccountValid))
  }

  def findAddress(postCode: String) = NoCacheAction.async { implicit request =>
    new GetAddressIOService().find(postCode).map { result =>
      // Capital A 'Addresses' is for compatibility with the https://api.getaddress.io/v2/uk/ response,
      // should a client want not to proxy via this server.
      Ok(Json.obj("Addresses" -> result.Addresses))
    } recover {
      case error if error.getMessage == "Bad Request" || error.getMessage == "Not Found" =>
        BadRequest //The postcode was invalid
      case error =>
        SafeLogger.error(scrub"Failed to complete postcode lookup via getAddress.io due to: $error")
        InternalServerError
    }
  }
}
