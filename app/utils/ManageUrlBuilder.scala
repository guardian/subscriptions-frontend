package utils

import com.gu.memsub.Product

object ManageUrlBuilder {
  def paymentUrl(baseUrl: String, product: Product) = {
    productSpecificUrl(baseUrl, "payment", product)
  }

  def suspendUrl(baseUrl: String, product: Product) = {
      productSpecificUrl(baseUrl, "suspend", product)
  }

  def deliveryProblemsUrl(baseUrl: String, product: Product) = {
      productSpecificUrl(baseUrl, "delivery", product, "/records")
  }

  private def productSpecificUrl(baseUrl: String, path: String, product: Product, extraParts: String*)= {
    val productUrlPart = product match {
      case Product.Voucher => "voucher"
      case Product.Delivery => "homedelivery"
      case _: Product.ZDigipack => "digitalpack"
      case _: Product.Weekly => "guardianweekly"
      case _ => ""
    }
    s"$baseUrl/$path/${productUrlPart}${extraParts.mkString("")}"
  }
}
