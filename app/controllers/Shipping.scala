package controllers

import actions.CommonActions
import model.ShippingDetails
import play.api.mvc.{AnyContent, Request, Action, Controller}

object Shipping extends Controller with CommonActions {

  def index(request: Request[AnyContent], shippingDetails: ShippingDetails) =
    request.body.asFormUrlEncoded
      .flatMap(_.get("package").map(_.head))
      .map(_.split("-").last)
      .collect {
        case "everyday" => shippingDetails.everyday
        case "sixday" => shippingDetails.sixday
        case "weekend" => shippingDetails.weekend
        case "sunday" => shippingDetails.sunday
      }
      .flatten
      .map(Redirect(_))
      .getOrElse(Ok(views.html.shipping.index(shippingDetails)))

  val collectionPaperDigital = cachedAction { request =>
    index(request, ShippingDetails(
      name = "Paper + digital voucher subscription",
      method = "collection",
      packageType = "paper-digital",
      everyday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv7&skip=1"),
      sixday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv6&skip=1"),
      weekend = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=gv2&skip=1"),
      sunday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx01&title=ov1&skip=1")
    ))
  }

  val collectionPaper = cachedAction { request =>
    index(request, ShippingDetails(
      name = "Paper voucher subscription",
      method = "collection",
      packageType = "paper",
      everyday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv7&skip=1"),
      sixday = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv6&skip=1"),
      weekend = Some("https://www.guardiansubscriptions.co.uk/Voucher?prom=faa03&pkgcode=ukx00&title=gv2&skip=1")
    ))
  }

  val deliveryPaperDigital = cachedAction { request =>
    index(request, ShippingDetails(
      name = "Paper + digital home delivery subscription",
      method = "delivery",
      packageType = "paper-digital",
      everyday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY%2B"),
      sixday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY%2B"),
      weekend = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND%2B"),
      sunday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SUNDAY%2B")
    ))
  }

  val deliveryPaper = cachedAction { request =>
    index(request, ShippingDetails(
      name = "Paper home delivery subscription",
      method = "delivery",
      packageType = "paper",
      everyday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=EVERYDAY"),
      sixday = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=SIXDAY"),
      weekend = Some("https://www.guardiandirectsubs.co.uk/Delivery/details.aspx?package=WEEKEND")
    ))
  }

}
