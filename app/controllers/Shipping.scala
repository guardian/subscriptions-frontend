package controllers

import actions.CommonActions._
import model.Subscriptions._
import play.api.mvc._

object Shipping extends Controller {

  def index(subscriptionCollection: SubscriptionCollection) = {
    Ok(views.html.shipping.shipping(subscriptionCollection))
  }

  def viewCollectionPaperDigital() = CachedAction {
    index(SubscriptionCollection(
      title = "Paper + digital voucher subscription",
      description = "Save money on your newspapers and digital content. Plus start using the daily edition and premium live news app immediately.",
      method = "collection",
      packageType = "paper-digital",
      options = Seq(
        SubscriptionOption(
          "Everyday+", "£10.99", Some("54%"), "£47.62", "Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv7&skip=1"
        ),
        SubscriptionOption(
          "Sixday+", "£9.99", Some("53%"), "£43.29", "Guardian papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv6&skip=1"
        ),
        SubscriptionOption(
          "Weekend+", "£5.99", Some("60%"), "£25.96", "Saturday Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv2&skip=1"
        ),
        SubscriptionOption(
          "Sunday+", "£4.99", Some("60%"), "£21.62", "Observer paper, plus tablet editions and Premium mobile access",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=ov1&skip=1"
        )
      )
    ))
  }
  def viewCollectionPaper() = CachedAction {
    index(SubscriptionCollection(
      title = "Paper voucher subscription",
      description = "Save money on your newspapers.",
      method = "collection",
      packageType = "paper",
      options = Seq(
        SubscriptionOption(
          "Everyday", "£9.99", Some("32%"), "£43.29", "Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv7&skip=1"
        ),
        SubscriptionOption(
          "Sixday", "£8.49", Some("27%"), "£36.79", "Guardian papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv6&skip=1"
        ),
        SubscriptionOption(
          "Weekend", "£4.49", Some("20%"), "£19.46", "Saturday Guardian and Observer papers",
          "https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv2&skip=1"
        )
      )
    ))
  }

  def viewDeliveryPaperDigital() = CachedAction {
    index(SubscriptionCollection(
      title = "Paper + digital home delivery subscription",
      description = """|If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30am on Sunday.
        |Plus you can start using the daily edition and premium live news app immediately.""".stripMargin,
      method = "delivery",
      packageType = "paper-digital",
      options = Seq(
        SubscriptionOption(
          "Everyday+", "£14.99", None, "£64.96", "Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY%2B"
        ),
        SubscriptionOption(
          "Sixday+", "£12.99", None, "£56.29", "Guardian papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY%2B"
        ),
        SubscriptionOption(
          "Weekend+", "£7.99", None, "£34.62", "Saturday Guardian and Observer papers, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND%2B"
        ),
        SubscriptionOption(
          "Sunday+", "£5.99", None, "£25.96", "Observer paper, plus tablet editions and Premium mobile access",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SUNDAY%2B"
        )
      )
    ))
  }

  def viewDeliveryPaper() = CachedAction {
    index(SubscriptionCollection(
      title = "Paper home delivery subscription",
      description = "If you live within the M25 you can have your papers delivered by 7am Monday - Saturday and 8.30 on Sunday.",
      method = "delivery",
      packageType = "paper",
      options = Seq(
        SubscriptionOption(
          "Everyday", "£13.99", None, "£60.62", "Guardian and Observer papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY"
        ),
        SubscriptionOption(
          "Sixday", "11.49", None, "£49.79", "Guardian papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY"
        ),
        SubscriptionOption(
          "Weekend", "£6.49", None, "£28.12", "Saturday Guardian and Observer papers",
          "https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND"
        )
      )
    ))
  }

}
