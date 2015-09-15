package model


object Benefits {

  case class BenefitPackage(
    title: String,
    list: Seq[BenefitItem]
  ) {
    val primaryItems = list.take(3)
    val secondaryItems = list.filterNot(primaryItems.contains)
  }

  case class BenefitItem(title: String, description: String)

  val digitalPack = BenefitPackage("Daily Edition", Seq(
    BenefitItem("The full experience", "Your daily newspaper optimised for tablet"),
    BenefitItem("Complete supplements", "Including The Guide, Cook, Do Something, Observer Tech and Food Monthly"),
    BenefitItem("Video and audio content", "Embedded audio, video and image galleries — plus all our crosswords"),
    BenefitItem("Available offline", "Downloaded automatically by 4am every morning. Perfect for travel"),
    BenefitItem("Designed for tablets", "Specially adapted to your device, whether you're using iPad, Android or Kindle Fire"),
    BenefitItem("30-day archive", "A month of back issues. Take your time, sit back, and enjoy")
  ))

  val digitalPackVue = BenefitPackage("Daily Edition", Seq(
    BenefitItem("The full experience", "Your newspaper, delivered to your tablet by 4am every morning, ready for offline reading."),
    BenefitItem("Complete supplements", "Including The Guide, Cook, Do Something, Observer Tech and Food Monthly"),
    BenefitItem("Video and audio content", "Embedded audio, video and image galleries — plus all our crosswords"),
    BenefitItem("Free cinema tickets", "Get two free Vue cinema tickets ever month for  12 months when you subscribe. Treat a friend or indulge yourself twice a month, it’s up to you."),
    BenefitItem("Designed for tablets", "Specially adapted to your device, whether you're using iPad, Android or Kindle Fire"),
    BenefitItem("30-day archive", "A month of back issues. Take your time, sit back, and enjoy")
  ))

  val premiumTier = BenefitPackage("Guardian App Premium Tier", Seq(
    BenefitItem("Advert-free", "No distractions, just the journalism you love"),
    BenefitItem("Available offline", "Tap to read content offline. Perfect for travel"),
    BenefitItem("Premium content", "Crosswords, exclusive articles and new features"),
    BenefitItem("Adjustable text size", "Larger text for ease of reading or smaller text to take in a whole article in one swipe"),
    BenefitItem("Save for later", "Articles stored in your account and available offline"),
    BenefitItem("Plus…", "Breaking news alerts, customised home screen and commenting")
  ))

}
