package controllers
import actions.CommonActions._
import com.gu.i18n.CountryGroup.UK
import com.gu.memsub.subsv2.CatalogPlan
import model.Subscriptions._
import play.api.mvc._
import services.TouchpointBackend

object Shipping extends Controller {

  // no need to support test users here really as plans seldom change
  val catalog = TouchpointBackend.Normal.catalogService.unsafeCatalog

  private def index(subscriptionCollection: SubscriptionProduct, productSegment: String) = {
    Ok(views.html.shipping.shipping(subscriptionCollection, productSegment))
  }

  private def planToOptions(in: CatalogPlan.Paid): SubscriptionOption =
    SubscriptionOption(in.slug,
      in.name, in.charges.gbpPrice.amount * 12 / 52, in.saving.map(_.toString + "%"), in.charges.gbpPrice.amount, in.description,
      routes.Checkout.renderCheckout(UK.id, None, None, in.slug).url
    )

  def viewCollectionPaperDigital() = CachedAction {
    val segment = "paper-digital"
    index(CollectionSubscriptionProduct(
      title = "Paper + digital voucher subscription",
      description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
      altPackagePath = s"/delivery/$segment",
      options = catalog.voucher.list.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ), segment)
  }

  def viewCollectionPaper() = CachedAction {
    val segment = "paper"
    index(CollectionSubscriptionProduct(
      title = "Paper voucher subscription",
      description = "Save money on your newspapers.",
      altPackagePath = s"/delivery/$segment",
      options = catalog.voucher.list.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ), segment)
  }

  def viewDeliveryPaperDigital() = CachedAction {
    val segment = "paper-digital"
    index(DeliverySubscriptionProduct(
      title = "Paper + digital home delivery subscription",
      description = """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
                      |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
      altPackagePath = s"/collection/$segment",
      options = catalog.delivery.list.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ), segment)
  }

  def viewDeliveryPaper() = CachedAction {
    val segment = "paper"
    index(DeliverySubscriptionProduct(
      title = "Paper home delivery subscription",
      description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
      altPackagePath = s"/collection/$segment",
      options = catalog.delivery.list.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ), segment)
  }

}
