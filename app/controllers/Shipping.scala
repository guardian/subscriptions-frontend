package controllers

import actions.CommonActions._
import com.gu.i18n.CountryGroup
import com.gu.memsub.Digipack
import com.gu.subscriptions.{PhysicalProducts, ProductPlan}
import model.Subscriptions._
import play.api.mvc._
import services.TouchpointBackend
import utils.TestUsers.PreSigninTestCookie
import scalaz.syntax.std.boolean._

object Shipping extends Controller {

  // no need to support test users here really as plans seldom change
  val catalog = TouchpointBackend.Normal.catalogService.paperCatalog

  def index(subscriptionCollection: SubscriptionProduct) = {
    Ok(views.html.shipping.shipping(subscriptionCollection))
  }

  def planToOptions(in: ProductPlan[PhysicalProducts]): SubscriptionOption =
    SubscriptionOption(in.slug,
      in.name, in.priceGBP.amount * 12 / 52, in.saving.map(_.toString + "%"), in.priceGBP.amount, in.description,
      routes.Checkout.renderCheckout(CountryGroup.UK, None, None, in.slug).url
    )

  def viewCollectionPaperDigital() = CachedAction {
    val plans = List.empty //catalog.voucher.productPlans.filter(_.products.others.contains(Digipack)).map(planToOptions)
    val qssSixDayPlus = SubscriptionOption("sixday",
      "Sixday+", 10.99f, Some("30%"), 47.62f, "Guardian papers, plus tablet editions and Premium mobile access",
      "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv6&skip=1"
    )
    val sixdayPlus = catalog.voucher.productPlans.filter(p => p.slug == "voucher-sixday+").map(planToOptions).headOption.getOrElse(qssSixDayPlus)
    index(CollectionSubscriptionProduct(
      title = "Paper + digital voucher subscription",
      description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
      packageType = "paper-digital",
      options = plans.nonEmpty.option(plans).getOrElse(
        Seq(
          SubscriptionOption("everyday",
            "Everyday+", 11.99f, Some("36%"), 51.96f, "Guardian and Observer papers, plus tablet editions and Premium mobile access",
            "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv7&skip=1"
          ),
          sixdayPlus,
          SubscriptionOption("weekend",
            "Weekend+", 6.79f, Some("22%"), 29.42f, "Saturday Guardian and Observer papers, plus tablet editions and Premium mobile access",
            "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv2&skip=1"
          ),
          SubscriptionOption("sunday",
            "Sunday+", 5.09f, Some("12%"), 22.06f, "Observer paper, plus tablet editions and Premium mobile access",
            "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=ov1&skip=1"
          )
        )
      )))
  }

  def viewCollectionPaper() = CachedAction {
    val plans = List.empty //catalog.voucher.productPlans.filter(_.products.others.isEmpty).map(planToOptions)
    index(CollectionSubscriptionProduct(
      title = "Paper voucher subscription",
      description = "Save money on your newspapers.",
      packageType = "paper",
      options = plans.nonEmpty.option(plans).getOrElse(Seq(
        SubscriptionOption("everyday",
          "Everyday", 10.99f, Some("31%"), 47.62f, "Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv7&skip=1"
        ),
        SubscriptionOption("sixday",
          "Sixday", 9.49f, Some("26%"), 41.12f, "Guardian papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv6&skip=1"
        ),
        SubscriptionOption("weekend",
          "Weekend", 4.79f, Some("19%"), 20.76f, "Saturday Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv2&skip=1"
        )
      )
      )))
  }

  def viewDeliveryPaperDigital() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper + digital home delivery subscription",
      description = """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
                      |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
      packageType = "paper-digital",
      options = catalog.delivery.productPlans.filter(_.products.others.map(_._1).contains(Digipack)).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

  def viewDeliveryPaper() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper home delivery subscription",
      description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
      packageType = "paper",
      options = catalog.delivery.productPlans.filter(_.products.others.isEmpty).map(planToOptions).sortBy(_.weeklyPrice).reverse
    ))
  }

}
