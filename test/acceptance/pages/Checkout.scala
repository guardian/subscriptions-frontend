package acceptance.pages


import acceptance.Config.appUrl
import org.scalatest.selenium.Page

object Checkout extends Page {
  val url = s"$appUrl/checkout"
}
