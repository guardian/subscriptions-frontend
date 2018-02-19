package model

import com.gu.i18n.Country
import com.gu.memsub.SupplierCode

case class SubscriptionRequestData(ipCountry: Option[Country], supplierCode: Option[SupplierCode])
