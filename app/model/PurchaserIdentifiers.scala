package model

import com.gu.salesforce.ContactId
import services.IdMinimalUser

case class PurchaserIdentifiers(buyerContactId: ContactId, recipientContactId: ContactId, identityId: Option[IdMinimalUser]) {
  val description = s"""
      |Salesforce data: Account: ${buyerContactId.salesforceAccountId},
      |Buyer Contact: ${buyerContactId.salesforceContactId},
      |Recipient Contact: ${recipientContactId.salesforceContactId},
      |Identity ID: ${identityId.map(_.id).mkString},
      |Is gift? ${isGift}
    """.stripMargin.trim
  val isGift = buyerContactId.salesforceAccountId == recipientContactId.salesforceAccountId &&
    buyerContactId.salesforceContactId != recipientContactId.salesforceContactId
  override def toString: String = description
}
