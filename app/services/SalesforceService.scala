package services

import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.PersonalData
import play.api.libs.json.{JsObject, Json}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait SalesforceService extends LazyLogging {
  def repo: SalesforceRepo

  def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[MemberId]

  def createSalesforceUserData(personalData: PersonalData): JsObject = Json.obj(
      Keys.EMAIL -> personalData.email,
      Keys.FIRST_NAME -> personalData.firstName,
      Keys.LAST_NAME -> personalData.lastName,
      Keys.MAILING_STREET -> personalData.address.address2,
      Keys.MAILING_CITY -> personalData.address.town,
      Keys.MAILING_POSTCODE -> personalData.address.postcode,
      Keys.MAILING_COUNTRY -> "United Kingdom",
      Keys.ALLOW_MEMBERSHIP_MAIL -> true,
      Keys.ALLOW_THIRD_PARTY_EMAIL -> true,
      Keys.ALLOW_GU_RELATED_MAIL -> true)

}

class SalesforceServiceImp(val repo: SalesforceRepo) extends SalesforceService {
  override def createOrUpdateUser(personalData: PersonalData, userId: UserId): Future[MemberId] =
    repo.upsert(userId.id, createSalesforceUserData(personalData))
}

class SalesforceRepo(salesforceConfig: SalesforceConfig) extends MemberRepository {
  override val salesforce = new Scalaforce {
    override val consumerKey = salesforceConfig.consumerKey
    override val apiUsername = salesforceConfig.apiUsername
    override val consumerSecret = salesforceConfig.consumerSecret
    override val apiToken = salesforceConfig.apiToken
    override val apiPassword = salesforceConfig.apiPassword
    override val application = Config.appName
    override val apiURL =salesforceConfig.apiURL.toString
    override val stage = salesforceConfig.envName

    val authTask = ScheduledTask("", Authentication("", ""), 0.seconds, 30.minutes)(getAuthentication)

    def authentication: Authentication = authTask.get()
  }
}

case class SalesforceServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
