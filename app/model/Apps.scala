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

  object DailyEdition {
    val iOSAppLink = AppLink(
      "https://itunes.apple.com/gb/app/guardian-observer-daily-edition/id452707806?mt=8&uo=4",
      appleAppStoreBadge
    )
  }

  object LiveNews {
    val links = Seq(
      AppLink(
        "https://play.google.com/store/apps/details?id=com.guardian",
        googlePlayStoreBadge
      ),
      AppLink(
        "https://itunes.apple.com/gb/app/the-guardian/id409128287?mt=8&uo=4",
        appleAppStoreBadge
      )
    )
  }

}
