package tracking.activities

import java.util.{Map => JMap}
import com.gu.i18n.CountryGroup
import tracking.TrackerData
import scala.collection.JavaConversions._


case class CheckoutReachedActivity(countryGroup: CountryGroup) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    Map(
      "eventSource" -> source,
      "country" -> countryGroup.name
    )
  }

  override def source: String = "checkoutReached"
}
