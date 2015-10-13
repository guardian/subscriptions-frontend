package tracking

import java.util.{List => JList, Map => JMap}

import com.github.t3hnar.bcrypt._
import com.snowplowanalytics.snowplow.tracker.core.emitter.{HttpMethod, RequestMethod}
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import controllers.Testing
import play.api.Logger
import play.api.mvc.RequestHeader
import utils.TestUsers.{TestUserCredentialType, isTestUser}

import scala.collection.JavaConversions._


trait TrackerData {
  def source: String
  def toMap: JMap[String, Object]
}


trait ActivityTracking {

  def trackAnon(data: TrackerData)(implicit request: RequestHeader) {
    val analyticsOff = request.cookies.get(Testing.AnalyticsCookieName).isDefined
    if (!analyticsOff) executeTracking(data)
  }

  def track[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C, data: TrackerData)(implicit request: RequestHeader) {
    isTestUser(permittedAltCredentialType, altCredentialSource).map(_ => executeTracking(data))
  }

  private def executeTracking(data: TrackerData) {
    try {
      val tracker = getTracker
      val dataMap = data.toMap
      tracker.trackUnstructuredEvent(dataMap)
    } catch {
      case error: Throwable => Logger.error(s"Activity tracking error: ${error.getMessage}")
    }
  }

  private def getTracker: Tracker = {
    val emitter = new Emitter(ActivityTracking.url, HttpMethod.GET)
    emitter.setRequestMethod(RequestMethod.Asynchronous)
    val subject = new Subject
    new Tracker(emitter, subject, "subscriptions", "subscriptions-frontend")
  }
}

object ActivityTracking {
  val url = Config.trackerUrl
  def setSubMap(in: Map[String, Any]): JMap[String, Object] =
     mapAsJavaMap(in).asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]
}
