package model

import com.gu.identity.play.IdMinimalUser
import com.gu.salesforce.ContactId

case class PurchaserIdentifiers(contactId: ContactId, identityId: Option[IdMinimalUser]) {
  val description = (Seq(s"SalesforceContactID - ${contactId.salesforceContactId}") ++ identityId.map(id => s"IdentityID - $id")).mkString(" ")
  override def toString: String = description
  lazy val identityIdWithFallback = identityId.map(_.id).getOrElse(contactId.salesforceContactId)
}
