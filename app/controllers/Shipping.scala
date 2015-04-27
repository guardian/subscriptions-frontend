package controllers

import model.ShippingDetails
import play.api.mvc.{Action, Controller}

object Shipping extends Controller{

  def collectionPaperDigital = Action {
    Ok(views.html.shipping.index(ShippingDetails(
      name = "Paper + digital voucher subscription",
      method = "collection",
      packageType = "paper-digital",
      everyday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv7&skip=1"),
      sixday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv6&skip=1"),
      weekend = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv2&skip=1"),
      sunday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=ov1&skip=1")
    )))
  }

  def collectionPaper = Action {
    Ok(views.html.shipping.index(ShippingDetails(
      name = "Paper voucher subscription",
      method = "collection",
      packageType = "paper",
      everyday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv7&skip=1"),
      sixday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv6&skip=1"),
      weekend = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv2&skip=1")
    )))
  }

  def deliveryPaperDigital = Action {
    Ok(views.html.shipping.index(ShippingDetails(
      name = "Paper + digital home delivery subscription",
      method = "delivery",
      packageType = "paper-digital",
      everyday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY%2B"),
      sixday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY%2B"),
      weekend = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND%2B"),
      sunday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SUNDAY%2B")
    )))
  }

  def deliveryPaper = Action {
    Ok(views.html.shipping.index(ShippingDetails(
      name = "Paper home delivery subscription",
      method = "delivery",
      packageType = "paper",
      everyday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY"),
      sixday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY"),
      weekend = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND")
    )))
  }

}
