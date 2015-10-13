package tracking.activities

import java.util.{Map => JMap}
import tracking.TrackerData
import scala.collection.JavaConversions._


case class CheckoutReachedActivity(country: String) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    Map(
      "eventSource" -> source,
      "country" -> country
    )
  }

  override def source: String = "checkoutReached"
}
