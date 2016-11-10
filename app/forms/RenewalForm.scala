package forms

import com.gu.memsub.{BillingPeriod, Quarter, Year}
import model._
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.data.format.Formatter


case class Renewal(email: String, subscriptionId: String, billingPeriod: BillingPeriod, paymentData: PaymentData)

object RenewalForm {


  implicit val pf1 = new Formatter[BillingPeriod] {
    val validPeriods = Seq(Quarter(), Year())
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BillingPeriod] =
      data.get(key).flatMap(periodName => validPeriods.find(_.noun == periodName) ).toRight(Seq(FormError(key, "invalid billing period")))

    override def unbind(key: String, value: BillingPeriod): Map[String, String] =
      Map(key -> value.noun)
  }

  //TODO copied from subs form move this to a common place to remove duplication
  val creditCardDataMapping = mapping("token" -> text)(CreditCardData)(CreditCardData.unapply)
  val directDebitDataMapping = mapping(
    "account" -> text(6, 10),
    "sortcode" -> text(6, 8),
    "holder" -> text
  )(DirectDebitData.apply)(DirectDebitData.unapply)
  implicit val paymentFormatter: Formatter[PaymentData] = new Formatter[PaymentData] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PaymentData] = {
      val methodKey = s"$key.type"
      data.get(methodKey).flatMap(PaymentType.fromKey) match {
        case Some(CreditCard) => creditCardDataMapping.withPrefix(key).bind(data)
        case Some(DirectDebit) => directDebitDataMapping.withPrefix(key).bind(data)
        case Some(_) => Left(Seq(FormError(methodKey, "valid values are credit-card and direct-debit")))
        case _ => Left(Seq(FormError(methodKey, "missing")))
      }
    }
    override def unbind(key: String, value: PaymentData): Map[String, String] = {
      val unPrefixed = value match {
        case CreditCardData(stripeToken) => Seq("type" -> CreditCard.toKey)
        case DirectDebitData(account, sortCode, holder) =>
          Seq(
            "account" -> account,
            "sortcode" -> sortCode,
            "holder" -> holder,
            "type" -> DirectDebit.toKey
          )
      }
      unPrefixed.map { case (k, v) => s"$key.$k" -> v }.toMap
    }
  }

  private val emailMaxLength = 240

  val mappings: Form[Renewal] = Form(
    mapping(
      "email" -> email.verifying("This email is too long", _.length < emailMaxLength + 1),
      "subscriptionId" ->text(1, 50), //TODO SHOULD I RESTRICT THIS TO THE LENGHT OF CURRENT IDS?
      "billingPeriod" -> of[BillingPeriod],
      "payment" -> of[PaymentData]
    )(Renewal.apply)(Renewal.unapply)
  )

}
