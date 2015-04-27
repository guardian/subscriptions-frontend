package model

case class ShippingDetails(name: String,
                   packageType: String,
                   method: String,
                   everyday: Option[String] = None,
                   sixday: Option[String] = None,
                   weekend: Option[String] = None,
                   sunday: Option[String] = None)
