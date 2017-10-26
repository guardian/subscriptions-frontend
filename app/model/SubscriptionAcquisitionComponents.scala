package model

import com.gu.acquisition.model.OphanIds
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.memsub.BillingPeriod
import com.gu.memsub.BillingPeriod.{Month, Quarter, SixMonths, Year}
import com.gu.stripe.Stripe.Charge
import ophan.thrift.event.PaymentProvider.{Gocardless, Stripe}
import ophan.thrift.event.{Acquisition, PaymentFrequency, Product}

case class SubscriptionAcquisitionComponents(subscribeRequest: SubscribeRequest)


object SubscriptionAcquisitionComponents {

  implicit object subscriptionAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[SubscriptionAcquisitionComponents] {

    def buildOphanIds(components: SubscriptionAcquisitionComponents): Either[String, OphanIds] = {
      import components._
      Right(OphanIds(pageviewId = Some("dummy"), visitId = Some("dummy"), browserId = Some("dummy")))
    }

    override def buildAcquisition(components: SubscriptionAcquisitionComponents): Either[String, Acquisition] = {
      import components._

      val plan = subscribeRequest.productData.fold(_.plan, _.plan)

      Right(
        Acquisition(
          product = subscribeRequest.productData match {
            case Left(_) => Product.PrintSubscription
            case Right(_) => Product.DigitalSubscription
          },

          paymentFrequency = plan.charges.billingPeriod match {
            case Month => PaymentFrequency.Monthly
            case Quarter => PaymentFrequency.Quarterly
            case SixMonths => PaymentFrequency.SixMonthly
            case Year => PaymentFrequency.Annually
          },

          currency = subscribeRequest.genericData.currency.iso,

          amount = plan.charges.price.getPrice(subscribeRequest.genericData.currency)
            .map(_.amount.toDouble).getOrElse(0),

          paymentProvider = subscribeRequest.genericData.paymentData match {
            case DirectDebitData(_, _, _) => Some(Stripe)
            case CreditCardData(_) => Some(Gocardless)
            case _ => None
          },

          countryCode = subscribeRequest.genericData.personalData.address.country.map(_.alpha2),

          // TODO: from request? session?
          campaignCode = None,
          abTests = None,
          referrerPageViewId = None,
          referrerUrl = None,
          componentId = None,
          componentTypeV2 = None,
          source = None
        )
      )
    }
  }
}
