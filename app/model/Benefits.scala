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
    BenefitItem("Complete supplements", "Including The Guide, Cook and Food Monthly"),
    BenefitItem("Video and audio content", "Embedded audio, video and image galleries — plus all our crosswords"),
    BenefitItem("Available offline", "Downloaded automatically by 4am every morning. Perfect for travel"),
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
