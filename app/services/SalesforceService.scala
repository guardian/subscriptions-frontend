package services

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.{DeliveryRecipient, NormalisedTelephoneNumber}
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import model.{PaperData, PersonalData}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait SalesforceService extends LazyLogging {
  def repo: SimpleContactRepository
  def createOrUpdateBuyerAndRecipient(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[ContactId]
  def isAuthenticated: Boolean
}

class SalesforceServiceImp(val repo: SimpleContactRepository) extends SalesforceService {
  override def createOrUpdateBuyerAndRecipient(personalData: PersonalData, paperData: Option[PaperData], userId: Option[IdMinimalUser]): Future[ContactId] = {
    for {
      buyerContact <- repo.upsert(userId.map(_.id), SalesforceService.getJSONForBuyerContact(personalData, paperData))
      sfContact <- { // Only create a Delivery / Recipient (Related) Contact if the recipient is a Giftee
        val gifteeRecipient = paperData.map(_.deliveryRecipient).filter(_.isGiftee)
        gifteeRecipient.map { recipient =>
          val recordTypeId = repo.recordTypes.getIdForContactRecordType(DeliveryRecipientContact)
          repo.upsert(userId.map(_.id), SalesforceService.getJSONForRecipientContact(buyerContact, recipient, recordTypeId))
        } getOrElse {
          Future.successful(buyerContact)
        }
      }
    } yield sfContact
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
  )) ++ paperData.map(_.deliveryRecipient).filterNot(_.isGiftee).map(_.address).fold(Json.obj())(addr => Json.obj(
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
    Keys.LAST_NAME -> deliveryRecipient.last,
    Keys.MAILING_STREET -> deliveryRecipient.address.line,
    Keys.MAILING_CITY -> deliveryRecipient.address.town,
    Keys.MAILING_POSTCODE -> deliveryRecipient.address.postCode,
    Keys.MAILING_STATE -> deliveryRecipient.address.countyOrState,
    Keys.MAILING_COUNTRY -> deliveryRecipient.address.country.fold(deliveryRecipient.address.countryName)(_.name)
  )
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
