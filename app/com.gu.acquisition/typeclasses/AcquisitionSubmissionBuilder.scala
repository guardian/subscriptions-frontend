package com.gu.acquisition.typeclasses

import com.gu.acquisition.model.errors.AnalyticsServiceError.BuildError
import com.gu.acquisition.model.{AcquisitionSubmission, GAData, OphanIds}
import ophan.thrift.event.Acquisition
import simulacrum.typeclass

/**
  * Type class for creating an acquisition submission from an arbitrary data type.
  */
@typeclass trait AcquisitionSubmissionBuilder[A] {

  import cats.syntax.either._

  def buildOphanIds(a: A): Either[String, OphanIds]

  def buildAcquisition(a: A): Either[String, Acquisition]

  def buildGAData(a: A): Either[String, GAData]

  def asAcquisitionSubmission(a: A): Either[BuildError, AcquisitionSubmission] =
    (for {
      ophanIds <- buildOphanIds(a)
      gaData <- buildGAData(a)
      acquisition <- buildAcquisition(a)
    } yield AcquisitionSubmission(ophanIds, gaData, acquisition)).leftMap(BuildError)
}
