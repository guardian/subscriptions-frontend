package tracking

import java.util.{List => JList, Map => JMap}

import com.github.t3hnar.bcrypt._
import com.snowplowanalytics.snowplow.tracker.core.emitter.{HttpMethod, RequestMethod}
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import controllers.Testing
import model.SubscriptionData
import model.zuora.{BillingFrequency, SubscriptionProduct}
import play.api.Logger
import play.api.mvc.RequestHeader
import services.CheckoutService.CheckoutResult
import utils.TestUsers.TestUserCredentialType

import scala.collection.JavaConversions._


trait TrackerData {
  def source: String
  def toMap: JMap[String, Object]
}

case class CheckoutReachedActivity(country: String) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    Map(
      "eventSource" -> source,
      "country" -> country
    )
  }

  override def source: String = "checkoutReached"
}


//TODO: Shouldn't be passing CheckoutResult etc here, just doing this here now for clarity of whats needed
case class MemberData(checkoutResult: CheckoutResult, subscriptionData: SubscriptionData, product: SubscriptionProduct) {

  def toMap: JMap[String, Any] = {

    def bcrypt(string: String) = (string + Config.bcryptPepper).bcrypt(Config.bcryptSalt)


    val address = subscriptionData.personalData.address

    val subscriptionPlan = Map("subscriptionPlan" -> (product.frequency match {
      case BillingFrequency.Month => "monthly"
      case BillingFrequency.Quarter => "quaterly"
      case BillingFrequency.Annual => "annual"
    }))

    val marketingChoices = Map("marketingChoicesForm" -> ActivityTracking.setSubMap(Map(
      "gnm" -> subscriptionData.personalData.receiveGnmMarketing,
      "membership" -> false,
      "thirdParty" -> false
    )))

    val identityData = Map(
      "salesforceContactId" -> bcrypt(checkoutResult.salesforceMember.salesforceContactId),
      "identityId" -> bcrypt(checkoutResult.userIdData.id.toString)
    )

    val addressData = Map(
      "billingPostcode" -> truncatePostcode(address.postCode),
      "deliveryPostcode" -> truncatePostcode(address.postCode),
      "city" -> address.town,
      "country" -> address.country.name
    )

    addressData ++ marketingChoices ++ subscriptionPlan ++ identityData
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length-3)._1.trim
  }

}

case class SubscriptionCreatedActivity(memberData: MemberData) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    println(memberData.toMap)
    ActivityTracking.setSubMap(Map("eventSource" -> source) ++ memberData.toMap)
  }
  override def source: String = "subscriptionCreated"
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
