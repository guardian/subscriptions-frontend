package tracking.activities

import java.util.{Map => JMap}
import com.github.t3hnar.bcrypt._
import com.gu.membership.zuora.Address
import configuration.Config
import model.SubscriptionData
import model.zuora.{BillingFrequency, SubscriptionProduct}
import services.CheckoutService.CheckoutResult
import tracking.{ActivityTracking, TrackerData}
import scala.collection.JavaConversions._

object MemberData {
  def apply(checkoutResult: CheckoutResult, subscriptionData: SubscriptionData, product: SubscriptionProduct): MemberData = {
    val address: Address = subscriptionData.personalData.address
      MemberData(address.town,
      address.country.name,
      address.postCode,
      product.frequency,
      subscriptionData.personalData.receiveGnmMarketing,
      checkoutResult.salesforceMember.salesforceContactId,
      checkoutResult.userIdData.id.toString
    )
  }
}

case class MemberData(town: String, country: String, postCode: String, billingFrequency: BillingFrequency, receiveGnmMarketing: Boolean, salesForceContactId: String, userId: String) {

  def toMap: JMap[String, Any] = {

    def bcrypt(string: String) = (string + Config.bcryptPepper).bcrypt(Config.bcryptSalt)

    val subscriptionPlan = Map("subscriptionPlan" -> (billingFrequency match {
      case BillingFrequency.Month => "monthly"
      case BillingFrequency.Quarter => "quaterly"
      case BillingFrequency.Annual => "annual"
    }))

    val marketingChoices = Map("marketingChoicesForm" -> ActivityTracking.setSubMap(Map(
      "gnm" -> receiveGnmMarketing,
      "membership" -> false,
      "thirdParty" -> false
    )))

    val identityData = Map(
      "salesforceContactId" -> bcrypt(salesForceContactId),
      "identityId" -> bcrypt(userId)
    )

    val addressData = Map(
      "billingPostcode" -> truncatePostcode(postCode),
      "deliveryPostcode" -> truncatePostcode(postCode),
      "city" -> town,
      "country" -> country
    )

    val tierData = Map(
      "tier" -> "digital"
    )

    addressData ++ marketingChoices ++ subscriptionPlan ++ identityData ++ tierData
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length - 3)._1.trim
  }

}

case class SubscriptionRegistrationActivity(memberData: MemberData) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    println(memberData.toMap)
    ActivityTracking.setSubMap(Map("eventSource" -> source) ++ memberData.toMap)
  }

  override def source: String = "subscriptionRegistration"
}
