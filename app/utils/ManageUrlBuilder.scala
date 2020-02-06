package utils

import com.gu.memsub.Product

object ManageUrlBuilder {
  def paymentUrl(baseUrl: String, product: Product) = {
    productSpecificUrl(baseUrl, "payment", product)
  }

  def suspendUrl(baseUrl: String, product: Product) = {
      productSpecificUrl(baseUrl, "suspend", product)
  }

  private def productSpecificUrl(baseUrl: String, path: String, product: Product )= {
    product match {
      case Product.Voucher => s"$baseUrl/$path/voucher"
      case Product.Delivery => s"$baseUrl/$path/homedelivery"
      case _: Product.ZDigipack => s"$baseUrl/$path/digitalpack"
      case _: Product.Weekly => s"$baseUrl/$path/guardianweekly"
      case _ => ""
    }
  }
}
