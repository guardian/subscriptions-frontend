package tracking.activities

import sun.tools.jmap.JMap
import tracking.TrackerData


case class CheckoutReachedActivity(country: String) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    Map(
      "eventSource" -> source,
      "country" -> country
    )
  }

  override def source: String = "checkoutReached"
}
