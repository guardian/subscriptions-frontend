package model

import com.gu.identity.play.IdMinimalUser
import com.gu.salesforce.ContactId

case class PurchaserIdentifiers(memberId: ContactId, identityId: Option[IdMinimalUser]) {
  val description = (Seq(s"SalesforceContactID - ${memberId.salesforceContactId}") ++ identityId.map(id => s"IdentityID - $id")).mkString(" ")
  override def toString: String = description
}
