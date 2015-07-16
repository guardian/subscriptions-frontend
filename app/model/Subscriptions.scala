package model

object Subscriptions {

  case class SubscriptionOption(
    title: String,
    weeklyPrice: String,
    weeklySaving: Option[String],
    monthlyPrice: String,
    description: String,
    url: String
  )

  case class SubscriptionCollection(
    title: String,
    description: String,
    packageType: String,
    method: String,
    options: Seq[SubscriptionOption]
  ) {
    val isCollection = method == "collection"
    val isPaperDigital = packageType == "paper-digital"
    val capitalizedTitle = title.split("\\s").map(_.capitalize).mkString(" ")
  }

}

