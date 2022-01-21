package model

import com.gu.memsub.Product
import com.gu.memsub.Product.{Delivery, Voucher}
import play.twirl.api.Html

object Subscriptions {

  case class SubscriptionOption(
    id: String,
    title: String,
    weeklyPrice: Float,
    weeklySaving: Option[String],
    monthlyPrice: Float,
    description: String,
    url: String,
    paymentDetails: Option[Html] = None
  )

  case class SubscriptionProductFooter(text: String, url: String)

  trait SubscriptionProduct {
    val id: String
    val title: String
    val description: String
    val altPackagePath: String
    val options: Seq[SubscriptionOption]
    val stepsHeading: String
    val steps: Seq[String]
    val stepsFooter: SubscriptionProductFooter
    val insideM25: Boolean
    val isDiscounted: Boolean
    val catalogProduct: Product
    def capitalizedTitle = title.split("\\s").map(_.capitalize).mkString(" ")
  }

  case class DeliverySubscriptionProduct(
    title: String,
    description: String,
    altPackagePath: String,
    options: Seq[SubscriptionOption],
    isDiscounted: Boolean = false
  ) extends SubscriptionProduct {
    override val catalogProduct = Product.Delivery
    override val insideM25 = true
    override val id = Delivery.name
    val stepsHeading = "This is how direct delivery works"
    val stepsFooter  = SubscriptionProductFooter("Not within the M25? Click here for our voucher scheme", "https://sub.thegulocal.com/p/Waitrose#voucher")
    val steps = Seq(
      "Pick the perfect package for you",
      "Confirm your address is within the M25",
      "Get your papers delivered to your door by 8am Monday - Saturday and 8:30am on Sundays"
    )
  }

  case class CollectionSubscriptionProduct(
    title: String,
    description: String,
    altPackagePath: String,
    options: Seq[SubscriptionOption],
    isDiscounted: Boolean = false
  ) extends SubscriptionProduct {
    override val catalogProduct = Product.Voucher
    override val insideM25 = false
    override val id = Voucher.name
    val stepsHeading = "This is how the voucher scheme works"
    val stepsFooter  = SubscriptionProductFooter("For Home Delivery within the M25 click here", "https://sub.thegulocal.com/p/Waitrose#delivery")
    val steps = Seq(
      "Pick the perfect package for you",
      "We'll post personalised vouchers for the newspapers in your package",
      "Take your vouchers to your newsagent or where you buy your paper"
    )
  }

}

