package model

import com.gu.acquisition.model.OphanIds
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import com.gu.stripe.Stripe.Charge
import ophan.thrift.event.{Acquisition, PaymentFrequency, Product}

case class SubscriptionAcquisitionComponents(subscribeRequest: SubscribeRequest)


object SubscriptionAcquisitionComponents {

  implicit object subscriptionAcquisitionSubmissionBuilder
    extends AcquisitionSubmissionBuilder[SubscriptionAcquisitionComponents] {

    def buildOphanIds(components: SubscriptionAcquisitionComponents): Either[String, OphanIds] = {
      import components._
      Right(OphanIds(pageviewId = "dummy", visitId = Some("dummy"), browserId = Some("dummy")))
    }

    override def buildAcquisition(components: SubscriptionAcquisitionComponents): Either[String, Acquisition] = {
      import components._

      Right(
        Acquisition(
          product = subscribeRequest.productData match {
            // TODO: THIS SHOULD BE PrintSubscription
            case Left(_) => Product.PaperSubscriptionEveryday
            case Right(_) => Product.DigitalSubscription
          },
          paymentFrequency = PaymentFrequency.OneOff,
          currency = "dummy",
          // Stripe amount is in smallest currency unit.
          // Convert e.g. Pence to Pounds, Cents to Dollars
          // https://stripe.com/docs/api#charge_object
          amount = 12.5,
          amountInGBP = None, // Calculated at the sinks of the Ophan stream
          paymentProvider = None,
          campaignCode = None,
          abTests = None,
          countryCode = None,
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
