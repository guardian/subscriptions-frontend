package services

import model.SubscriptionData

import scala.concurrent.Future

object CheckoutService {
  def processSubscription(subscriptionData: SubscriptionData): Future[String] = Future.successful("AB123456")
}
