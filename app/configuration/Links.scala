package configuration

case class Links(href: String, title: String)

object Links {
  val terms = Links(
    "http://www.theguardian.com/info/2014/aug/06/guardian-observer-digital-subscriptions-terms-conditions",
    "Terms and Conditions"
  )
  val privacyPolicy = Links(
    "http://www.theguardian.com/help/privacy-policy",
    "Privacy Policy"
  )

  val whyYourDataMattersToUs = Links(
    "https://www.theguardian.com/info/video/2014/nov/03/why-your-data-matters-to-us-video",
    "Why your data matter to us"
  )

  val guardianSubscriptionFaqs = Links(
    "http://www.theguardian.com/subscriber-direct/subscription-frequently-asked-questions",
    "Frequently Asked Questions"
  )
}
