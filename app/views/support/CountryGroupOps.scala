package views.support

import com.gu.i18n.CountryGroup
import model.SupportPhoneDetails

/**
  * Created by alex_ware on 18/01/2016.
  */
object CountryGroupOps {
 implicit class CountryGroupWithSupportPhone(countryGroup: CountryGroup) {
   def phoneDetails: Option[SupportPhoneDetails] = countryGroup match {
     case CountryGroup.UK => Some(SupportPhoneDetails.UK)
     case _ => None
   }
 }
}
