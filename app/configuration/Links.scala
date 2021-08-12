package configuration

import utils.StringUtils._

case class Links(href: String, title: String) {
  val slug = slugify(title)
}

object Links {
  val digipackTerms = Links(
    "https://www.theguardian.com/info/2014/aug/06/guardian-observer-digital-subscriptions-terms-conditions",
    "Terms and Conditions"
  )
  val paperTerms = Links(
    "https://www.theguardian.com/subscriber-direct/subscription-terms-and-conditions",
    "Terms and Conditions"
  )
  val weeklyTerms = Links(
    "https://www.theguardian.com/info/2014/jul/10/guardian-weekly-print-subscription-services-terms-conditions",
    "Terms and Conditions"
  )
  val privacyPolicy = Links(
    "https://www.theguardian.com/help/privacy-policy",
    "Privacy Policy"
  )

}

object ProfileLinks {
  private val identityUrl = Config.Identity.webAppUrl

  val signIn =  Links(s"$identityUrl/signin?returnUrl=${Config.subscriptionsUrl}", "Your account")

  val commentActivity = Links(s"$identityUrl/user/id/", "Comment activity")
  val editProfile = Links(s"$identityUrl/public/edit", "Edit profile")
  val emailPreferences = Links(s"$identityUrl/email-prefs", "Email preferences")
  val changePassword = Links(s"$identityUrl/reset", "Change password")
  val signOut = Links(s"$identityUrl/signout", "Sign out")

  val popupLinks = Seq(commentActivity, editProfile, emailPreferences, changePassword, signOut)
}
