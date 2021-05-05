package com.gu.acquisition.model

import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder
import ophan.thrift.event.Acquisition

/**
  * Encapsulates all the data required to submit an acquisition to Ophan.
  */
case class AcquisitionSubmission(ophanIds: OphanIds, gaData: GAData, acquisition: Acquisition)

object AcquisitionSubmission {

  // Type class allows an acquisition submission to passed directly to the submit() method
  // of an Ophan service instance.
  implicit val acquisitionSubmissionBuilder: AcquisitionSubmissionBuilder[AcquisitionSubmission] =
    new AcquisitionSubmissionBuilder[AcquisitionSubmission] {

      override def buildOphanIds(a: AcquisitionSubmission): Either[String, OphanIds] = Right(a.ophanIds)

      override def buildGAData(a: AcquisitionSubmission): Either[String, GAData] = Right(a.gaData)

      override def buildAcquisition(a: AcquisitionSubmission): Either[String, Acquisition] = Right(a.acquisition)
    }
}
