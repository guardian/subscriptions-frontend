package views.support
import com.gu.memsub.Address

object AddressOps {
  implicit class AddressChecker(in:Address) {
    def isEmpty = (in.lineOne + in.lineTwo + in.town + in.countyOrState + in.postCode + in.countryName).isEmpty
  }
}
