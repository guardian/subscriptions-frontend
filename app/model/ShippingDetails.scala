package model

case class ShippingDetails(name: String,
                           packageType: String,
                           method: String,
                           everyday: Option[String] = None,
                           sixday: Option[String] = None,
                           weekend: Option[String] = None,
                           sunday: Option[String] = None) {
  val isCollection = method == "collection"
  val isPaperDigital = packageType == "paper-digital"
  val capitalizedName = name.split("\\s").map(_.capitalize).mkString(" ")
}
