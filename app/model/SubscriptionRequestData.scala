package model

import java.net.InetAddress

import com.gu.memsub.SupplierCode

case class SubscriptionRequestData(ipAddress: Option[InetAddress], supplierCode: Option[SupplierCode])
