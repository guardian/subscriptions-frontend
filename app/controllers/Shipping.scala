package controllers
import actions.CommonActions
import com.gu.i18n.CountryGroup.UK
import com.gu.memsub.subsv2.{Catalog, CatalogPlan}
import model.Subscriptions._
import play.api.mvc._
import services.TouchpointBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Shipping(touchpointBackend: TouchpointBackend, commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions._
  // no need to support test users here really as plans seldom change
  val catalog: Future[Catalog] = touchpointBackend.catalogService.catalog.map(_.valueOr(e => throw new IllegalStateException(s"$e while getting catalog")))

  private def index(subscriptionCollection: SubscriptionProduct, productSegment: String) = {
    Ok(views.html.shipping.shipping(subscriptionCollection, productSegment))
  }

  private def planToOptions(in: CatalogPlan.Paid): SubscriptionOption =
    SubscriptionOption(in.slug,
      in.name, in.charges.gbpPrice.amount * 12 / 52, in.saving.map(_.toString + "%"), in.charges.gbpPrice.amount, in.description,
      routes.Checkout.renderCheckout(UK.id, None, None, in.slug).url
    )

  def viewCollectionPaperDigital() = NoCacheAction.async {
    val segment = "paper-digital"
    catalog.map { catalog =>
      index(CollectionSubscriptionProduct(
        title = "Paper + digital voucher subscription",
        description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
        altPackagePath = s"/delivery/$segment",
        options = catalog.voucher.list.toList.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
      ), segment)
    }
  }

  def viewCollectionPaper() = NoCacheAction.async {
    val segment = "paper"
    catalog.map { catalog =>
      index(CollectionSubscriptionProduct(
        title = "Paper voucher subscription",
        description = "Save money on your newspapers.",
        altPackagePath = s"/delivery/$segment",
        options = catalog.voucher.list.toList.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse.filterNot(_.title.toLowerCase == "saturday")
      ), segment)
    }
  }

  def viewDeliveryPaperDigital() = NoCacheAction.async {
    val segment = "paper-digital"
    catalog.map { catalog =>
      index(DeliverySubscriptionProduct(
        title = "Paper + digital home delivery subscription",
        description =
          """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
             |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
        altPackagePath = s"/collection/$segment",
        options = catalog.delivery.list.toList.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
      ), segment)
    }
  }

  def viewDeliveryPaper() = NoCacheAction.async {
    val segment = "paper"
    catalog.map { catalog =>
      index(DeliverySubscriptionProduct(
        title = "Paper home delivery subscription",
        description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
        altPackagePath = s"/collection/$segment",
        options = catalog.delivery.list.toList.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse.filterNot(_.title.toLowerCase == "saturday")
      ), segment)
    }
  }

}
