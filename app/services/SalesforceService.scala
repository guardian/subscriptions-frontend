package services

import com.gu.memsub.NormalisedTelephoneNumber
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import model.{DeliveryRecipient, PaperData, PersonalData, PurchaserIdentifiers}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SalesforceService extends LazyLogging {
  def repo: SimpleContactRepository
  def createOrUpdateBuyerAndRecipient(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[PurchaserIdentifiers]
  def isAuthenticated: Boolean
}

class SalesforceServiceImp(val repo: SimpleContactRepository) extends SalesforceService {
  val recordTypeId = repo.recordTypes.getIdForContactRecordType(DeliveryRecipientContact)

  private def createRecipientContactIfGift(buyerContact: ContactId, paperData: Option[PaperData]): Option[Future[ContactId]] = {
    // Only create a Delivery / Recipient (Related) Contact if the recipient has a giftee address
    paperData.map(_.deliveryRecipient).filter(_.gifteeAddress.nonEmpty).map { recipient =>
      repo.upsert(None, SalesforceService.getJSONForRecipientContact(buyerContact, recipient, recordTypeId))
    }
  }

  override def createOrUpdateBuyerAndRecipient(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[PurchaserIdentifiers] = {
    for {
      buyerContact <- repo.upsert(userId.map(_.id), SalesforceService.getJSONForBuyerContact(personalData, paperData))
      recipientContact <- createRecipientContactIfGift(buyerContact, paperData) getOrElse Future.successful(buyerContact)
    } yield PurchaserIdentifiers(buyerContact, recipientContact, userId)
  }

  override def isAuthenticated = repo.salesforce.isAuthenticated
}

object SalesforceService {
  def getJSONForBuyerContact(personalData: PersonalData, paperData: Option[PaperData]): JsObject = Json.obj(
    Keys.EMAIL -> personalData.email,
    Keys.FIRST_NAME -> personalData.first,
    Keys.LAST_NAME -> personalData.last,
    Keys.BILLING_STREET -> personalData.address.line,
    Keys.BILLING_CITY -> personalData.address.town,
    Keys.BILLING_POSTCODE -> personalData.address.postCode,
    Keys.BILLING_COUNTRY -> personalData.address.country.fold(personalData.address.countryName)(_.name),
    Keys.BILLING_STATE -> personalData.address.countyOrState,
    Keys.ALLOW_GU_RELATED_MAIL -> personalData.receiveGnmMarketing
  ) ++ personalData.title.fold(Json.obj())(title => Json.obj(
    Keys.TITLE -> title.title
  )) ++ paperData.flatMap(_.deliveryRecipient.buyersMailingAddress).fold(Json.obj())(addr => Json.obj(
    Keys.MAILING_STREET -> addr.line,
    Keys.MAILING_CITY -> addr.town,
    Keys.MAILING_POSTCODE -> addr.postCode,
    Keys.MAILING_STATE -> addr.countyOrState,
    Keys.MAILING_COUNTRY -> addr.country.fold(addr.countryName)(_.name)
  ) ++ paperData.flatMap(_.sanitizedDeliveryInstructions).fold(Json.obj())(instrs => Json.obj(
    Keys.DELIVERY_INSTRUCTIONS -> instrs
  ))) ++ NormalisedTelephoneNumber.fromStringAndCountry(personalData.telephoneNumber, personalData.address.country).fold(Json.obj())(phone => Json.obj(Keys.TELEPHONE -> phone.format))

  def getJSONForRecipientContact(buyerContact: ContactId, deliveryRecipient: DeliveryRecipient, recordTypeId: String): JsObject = Json.obj(
    Keys.ACCOUNT_ID -> buyerContact.salesforceAccountId,
    Keys.RECORD_TYPE_ID -> recordTypeId,
    Keys.EMAIL -> deliveryRecipient.email,
    Keys.TITLE -> deliveryRecipient.title.map(_.title).mkString,
    Keys.FIRST_NAME -> deliveryRecipient.first,
    Keys.LAST_NAME -> deliveryRecipient.last
  ) ++ deliveryRecipient.gifteeAddress.fold(Json.obj())(addr => Json.obj(
    Keys.MAILING_STREET -> addr.line,
    Keys.MAILING_CITY -> addr.town,
    Keys.MAILING_POSTCODE -> addr.postCode,
    Keys.MAILING_STATE -> addr.countyOrState,
    Keys.MAILING_COUNTRY -> addr.country.fold(addr.countryName)(_.name)
  ))
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
