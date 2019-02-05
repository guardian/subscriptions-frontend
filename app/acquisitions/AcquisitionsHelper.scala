package acquisitions

import com.gu.acquisition.model.ReferrerAcquisitionData
import com.typesafe.scalalogging.LazyLogging
import ophan.thrift.event.AbTest
import play.api.libs.json.{JsError, Json}
import scalaz.\/

import scala.collection.immutable.Set

object AcquisitionsHelper extends LazyLogging {
  def referrerAcquisitionDataFromJSON(json: String): Option[ReferrerAcquisitionData] = {
    import \/._

    fromTryCatchNonFatal(Json.parse(json))
      .leftMap(err => s"""Unable to parse "$json" as JSON. $err""")
      .flatMap { jsValue =>
        Json.fromJson[ReferrerAcquisitionData](jsValue).fold(
          errs => left(logger.warn(s"Unable to decode JSON $jsValue to an instance of ReferrerAcquisitionData. ${JsError.toJson(errs)}")),
          referrerAcquisitionData => right(referrerAcquisitionData)
        )
      }
      .toOption
  }

  def abTestFromQueryParam(param: String): AbTest = {
    val Array(_, testName, testVariant) = param.split('.')
    new AbTest {
      override def name: String = s"optimize$$$$$testName"

      override def variant: String = testVariant

      override def complete: Option[Boolean] = None

      override def campaignCodes: Option[collection.Set[String]] = None
    }
  }

  def mergeAcquisitionDataJson(maybeAcquisitionDataJson: Option[String], optimizeData: Option[String]): Option[String] =
    mergeAcquisitionData(maybeAcquisitionDataJson: Option[String], optimizeData: Option[String])
      .map(
        referrerAcquisitionData =>
          Json.toJson(referrerAcquisitionData).toString()
      )

  def mergeAcquisitionData(maybeAcquisitionDataJson: Option[String], optimizeData: Option[String]): Option[ReferrerAcquisitionData] = {

    val appendToExisting = for {
      acquisitionDataJson <- maybeAcquisitionDataJson
      acquisitionData <- referrerAcquisitionDataFromJSON(acquisitionDataJson)
      optimizeExperimentParam <- optimizeData
      newTests = acquisitionData.abTests.getOrElse(Set.empty[AbTest]) + abTestFromQueryParam(optimizeExperimentParam)
    } yield acquisitionData.copy(abTests = Some(newTests))

    appendToExisting.orElse(
      optimizeData.map(
        optimizeExperimentParam =>
          new ReferrerAcquisitionData(
            campaignCode = None,
            referrerPageviewId = None,
            referrerUrl = None,
            componentId = None,
            componentType = None,
            source = None,
            abTest = None,
            abTests = Some(Set(abTestFromQueryParam(optimizeExperimentParam))),
            queryParameters = None
          )
      )
    )
  }

}
