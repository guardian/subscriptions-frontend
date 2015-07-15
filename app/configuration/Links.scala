package configuration

case class Links(href: String, title: String)

object Links {
  val termsOfService = Links(
    "http://www.theguardian.com/info/2014/aug/06/guardian-observer-digital-subscriptions-terms-conditions",
    "Terms of Service"
  )
  val privacyPolicy = Links(
    "http://www.theguardian.com/info/2014/aug/06/guardian-observer-digital-subscriptions-terms-conditions",
    "Privacy Policy"
  )
  val guardianSubscriptionFaqs = Links(
    "http://www.theguardian.com/subscriber-direct/subscription-frequently-asked-questions",
    "Frequently Asked Questions"
  )
}
