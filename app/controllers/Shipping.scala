package controllers
import actions.CommonActions._
import com.gu.i18n.CountryGroup
import com.gu.memsub.subsv2.CatalogPlan
import model.Subscriptions._
import play.api.mvc._
import services.TouchpointBackend

object Shipping extends Controller {

  // no need to support test users here really as plans seldom change
  val catalog = TouchpointBackend.Normal.catalogService.unsafeCatalog

  def index(subscriptionCollection: SubscriptionProduct) = {
    Ok(views.html.shipping.shipping(subscriptionCollection))
  }

  def planToOptions(in: CatalogPlan.Paid): SubscriptionOption =
    SubscriptionOption(in.slug,
      in.name, in.charges.gbpPrice.amount * 12 / 52, in.saving.map(_.toString + "%"), in.charges.gbpPrice.amount, in.description,
      routes.Checkout.renderCheckout(CountryGroup.UK, None, None, in.slug).url
    )

  def viewCollectionPaperDigital() = CachedAction {
    index(CollectionSubscriptionProduct(
      title = "Paper + digital voucher subscription",
      description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
      altPackagePath = "/delivery/paper-digital",
      options = catalog.voucher.list.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

  def viewCollectionPaper() = CachedAction {
    index(CollectionSubscriptionProduct(
      title = "Paper voucher subscription",
      description = "Save money on your newspapers.",
      altPackagePath = "/delivery/paper",
      options = catalog.voucher.list.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

  def viewDeliveryPaperDigital() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper + digital home delivery subscription",
      description = """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
                      |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
      altPackagePath = "/collection/paper-digital",
      options = catalog.delivery.list.filter(_.charges.digipack.isDefined).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

  def viewDeliveryPaper() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper home delivery subscription",
      description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
      altPackagePath = "/collection/paper",
      options = catalog.delivery.list.filter(_.charges.digipack.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

}
