package com.gu.acquisition.services

import cats.data.EitherT
import com.gu.acquisition.model.AcquisitionSubmission
import com.gu.acquisition.model.errors.AnalyticsServiceError
import com.gu.acquisition.typeclasses.AcquisitionSubmissionBuilder

import scala.concurrent.{ExecutionContext, Future}

private [acquisition] trait AnalyticsService {
  def submit[A : AcquisitionSubmissionBuilder](a: A)(implicit ec: ExecutionContext): EitherT[Future, AnalyticsServiceError, AcquisitionSubmission]
}
