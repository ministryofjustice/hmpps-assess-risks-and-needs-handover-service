package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.response

import java.util.UUID

data class AssociationsResponse(
  val sanAssessmentId: UUID?,
  val sentencePlanId: UUID?,
)
