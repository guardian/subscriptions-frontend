package views.support
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.{DigitalEdition => DE}
import DE._

object DigitalEdition {

  implicit class DigitalEditionOps(edition: DE) {

    def redirect: Uri = edition match {
      case UK => "/digital/country"
      case _ => "https://www.guardiansubscriptions.co.uk/digitalsubscriptions/?prom=dga38&CMP=FAB_3062"
    }

    def membershipLandingPage: Uri = {
      val params = "INTCMP" -> s"GU_SUBSCRIPTIONS_${edition.id.toUpperCase}_PROMO"

      edition match {
        case INTL => "https://membership.theguardian.com/int/supporter" ? params
        case US => "https://membership.theguardian.com/us/supporter" ? params
        case _ => "https://membership.theguardian.com/join" ? params
      }
    }

    def digitalPackSaving = edition match {
      case US | AU | UK => Some(75)
      case INTL => None
    }

    def guardianWeeklySaving = edition match {
      case INTL | UK => None
      case US => Some(23)
      case AU => Some(9)
    }
  }

}
