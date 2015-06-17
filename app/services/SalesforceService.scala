package services

import com.gu.identity.play.IdUser
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce.{Authentication, MemberId, MemberRepository, Scalaforce}
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.libs.json.{JsObject, Json}
import utils.ScheduledTask

import scala.concurrent.Future
import scala.concurrent.duration._

trait SalesforceService extends LazyLogging {

  def createSFUser(personalData: PersonalData, idUser: IdUser): Future[MemberId]

  def createSalesforceUserData(personalData: PersonalData): JsObject = {
    Seq(Json.obj(
      Keys.EMAIL -> personalData.email,
      Keys.FIRST_NAME -> personalData.firstName,
      Keys.LAST_NAME -> personalData.lastName,
      Keys.MAILING_STREET -> personalData.address.street,
      Keys.MAILING_CITY -> personalData.address.town,
      Keys.MAILING_POSTCODE -> personalData.address.postcode,
      Keys.MAILING_COUNTRY -> "United Kingdom",
      Keys.ALLOW_MEMBERSHIP_MAIL -> true
    )) ++ Map(
      Keys.ALLOW_THIRD_PARTY_EMAIL -> Some(true),
      Keys.ALLOW_GU_RELATED_MAIL -> Some(true)
    ).collect { case (k, Some(v)) => Json.obj(k -> v) }
  }.reduce(_ ++ _)

}

object SalesforceService extends SalesforceService {
  override def createSFUser(personalData: PersonalData, idUser: IdUser): Future[MemberId] =
    SalesforceRepo.upsert(idUser.id, createSalesforceUserData(personalData))
}

object SalesforceRepo extends MemberRepository {
  override val salesforce = new Scalaforce {
    override val consumerKey = Config.Salesforce.consumerKey
    override val apiUsername = Config.Salesforce.apiUsername
    override val consumerSecret = Config.Salesforce.consumerSecret
    override val apiToken = Config.Salesforce.apiToken
    override val apiPassword = Config.Salesforce.apiPassword
    override val application = Config.appName
    override val apiURL = Config.Salesforce.apiURL.toString
    override val stage = Config.Salesforce.envName

    val authTask = ScheduledTask("", Authentication("", ""), 0.seconds, 30.minutes)(getAuthentication)

    def authentication: Authentication = authTask.get()
  }
}