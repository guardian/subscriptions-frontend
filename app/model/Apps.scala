package model

import controllers.CachedAssets.hashedPathFor

object Apps {

  case class AppBadge(title: String, path: String) {
    val src = hashedPathFor(path)
  }
  case class AppLink(href: String, badge: AppBadge)

  val appleAppStoreBadge = AppBadge(
    "Download from the App Store",
    "images/logos/apple-app-store.png"
  )
  val googlePlayStoreBadge = AppBadge(
    "Get it on Google Play",
    "images/logos/google-play-store.png"
  )

  val installTrackingParam = "referrer=utm_source%3Dsubscribe.theguardian.com%26utm_medium%3Dthank_you_page"

  object DailyEdition {
    val iOSAppLink = AppLink(
      s"https://itunes.apple.com/gb/app/guardian-observer-daily-edition/id452707806?mt=8&uo=4&$installTrackingParam",
      appleAppStoreBadge
    )
  }

  object LiveNews {
    val links = Seq(
      AppLink(
        s"https://play.google.com/store/apps/details?id=com.guardian&$installTrackingParam",
        googlePlayStoreBadge
      ),
      AppLink(
        s"https://itunes.apple.com/gb/app/the-guardian/id409128287?mt=8&uo=4&$installTrackingParam",
        appleAppStoreBadge
      )
    )
  }

}
