package tracking.activities

import java.util.{Map => JMap}

import com.github.t3hnar.bcrypt._
import com.gu.memsub.{Address, BillingPeriod}
import configuration.Config
import model._
import tracking.{ActivityTracking, TrackerData}
import model.error.CheckoutService._

import scala.collection.JavaConversions._

case class MemberData(checkoutResult: CheckoutSuccess, subscriptionData: SubscribeRequest) {

  def toMap: JMap[String, Any] = {

    def bcrypt(string: String) = (string + Config.bcryptPepper).bcrypt(Config.bcryptSalt)

    val billingAddress = subscriptionData.genericData.personalData.address
    val deliveryAddress = subscriptionData.genericData.personalData.address // TODO change to deliveryAddress once merged

    // TODO test that the billing period is correctly carried through, now that we are not getting plan from the catalog
    val subscriptionPlan = Map("subscriptionPlan" -> subscriptionData.productData.fold(_.plan.name, _.plan.billingPeriod.adjective))

    val marketingChoices = Map("marketingChoicesForm" -> ActivityTracking.setSubMap(Map(
      "gnm" -> subscriptionData.genericData.personalData.receiveGnmMarketing,
      "membership" -> false,
      "thirdParty" -> false
    )))

    val identityData = Map(
      "salesforceContactId" -> bcrypt(checkoutResult.salesforceMember.salesforceContactId),
      "identityId" -> bcrypt(checkoutResult.userIdData.map(_.id).mkString)
    )

    val addressData = Map(
      "billingPostcode" -> truncatePostcode(billingAddress.postCode),
      "deliveryPostcode" -> truncatePostcode(deliveryAddress.postCode),
      "city" -> billingAddress.town,
      "country" -> billingAddress.country
    )

    val tierData = Map(
      "tier" -> subscriptionData.productData.fold(
        x => "paper", // TODO change to s"${if (is a bundle) "bundle" else "paper"}-${if (home delivery) "delivery" else "voucher"}"
        _ => "digital"
      )
    )

    val paymentMethodData = Map(
      "paymentMethod" -> (subscriptionData.genericData.paymentData match {
        case d: DirectDebitData => "direct-debit"
        case c: CreditCardData => "credit-card"
      })
    )

    addressData ++ marketingChoices ++ subscriptionPlan ++ identityData ++ tierData ++ paymentMethodData
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length - 3)._1.trim
  }

}

case class SubscriptionRegistrationActivity(memberData: MemberData) extends TrackerData {
  override def toMap: JMap[String, Object] = {
    ActivityTracking.setSubMap(Map("eventSource" -> source) ++ memberData.toMap)
  }

  override def source: String = "subscriptionRegistration"
}
