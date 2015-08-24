package controllers

import actions.CommonActions._
import model.Subscriptions._
import play.api.mvc._

object Shipping extends Controller {

  def index(subscriptionCollection: SubscriptionProduct) = {
    Ok(views.html.shipping.shipping(subscriptionCollection))
  }

  def viewCollectionPaperDigital() = CachedAction {
    index(CollectionSubscriptionProduct(
      title = "Paper + digital voucher subscription",
      description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
      packageType = "paper-digital",
      options = Seq(
        SubscriptionOption("everyday",
          "Everyday+", 10.99f, Some("37%"), 47.62f, "Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv7&skip=1"
        ),
        SubscriptionOption("sixday",
          "Sixday+", 9.99f, Some("31%"), 43.29f, "Guardian papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv6&skip=1"
        ),
        SubscriptionOption("weekend",
          "Weekend+", 6.79f, Some("19%"), 29.42f, "Saturday Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=gv2&skip=1"
        ),
        SubscriptionOption("sunday",
          "Sunday+", 5.09f, Some("10%"), 22.06f, "Observer paper, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx01&title=ov1&skip=1"
        )
      )
    ))
  }

  def viewCollectionPaper() = CachedAction {
    index(CollectionSubscriptionProduct(
      title = "Paper voucher subscription",
      description = "Save money on your newspapers.",
      packageType = "paper",
      options = Seq(
        SubscriptionOption("everyday",
          "Everyday", 9.99f, Some("32%"), 43.29f, "Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv7&skip=1"
        ),
        SubscriptionOption("sixday",
          "Sixday", 8.49f, Some("27%"), 36.79f, "Guardian papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv6&skip=1"
        ),
        SubscriptionOption("weekend",
          "Weekend", 4.79f, Some("14%"), 20.76f, "Saturday Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa13&pkgcode=ukx00&title=gv2&skip=1"
        )
      )
    ))
  }

  def viewDeliveryPaperDigital() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper + digital home delivery subscription",
      description = """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
        |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
      packageType = "paper-digital",
      options = Seq(
        SubscriptionOption("everyday",
          "Everyday+", 14.49f, None, 62.79f, "Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY%2B"
        ),
        SubscriptionOption("sixday",
          "Sixday+", 12.99f, None, 56.29f, "Guardian papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY%2B"
        ),
        SubscriptionOption("weekend",
          "Weekend+", 7.79f, None, 33.76f, "Saturday Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND%2B"
        ),
        SubscriptionOption("sunday",
          "Sunday+", 6.09f, None, 26.39f, "Observer paper, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SUNDAY%2B"
        )
      )
    ))
  }

  def viewDeliveryPaper() = CachedAction {
    index(DeliverySubscriptionProduct(
      title = "Paper home delivery subscription",
      description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
      packageType = "paper",
      options = Seq(
        SubscriptionOption("everyday",
          "Everyday", 13.49f, None, 58.46f, "Guardian and Observer papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY"
        ),
        SubscriptionOption("sixday",
          "Sixday", 11.49f, None, 49.79f, "Guardian papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY"
        ),
        SubscriptionOption("weekend",
          "Weekend", 5.79f, None, 25.09f, "Saturday Guardian and Observer papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND"
        )
      )
    ))
  }

}
