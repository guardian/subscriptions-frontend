package tracking.activities

import java.util.{Map => JMap}

import tracking.TrackerData

import scala.collection.JavaConversions._


object QssRedirectActivity extends TrackerData {
  override def toMap: JMap[String, Object] = {
    Map(
      "eventSource" -> source
    )
  }
  override def source: String = "qssRedirected"
}
