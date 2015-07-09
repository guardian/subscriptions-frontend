package configuration

case class Links(href: String, title: String)

object Links {
  val guardianSubscriptionFaqs = Links(
    "http://www.theguardian.com/subscriber-direct/subscription-frequently-asked-questions",
    "Frequently Asked Questions"
  )
}
