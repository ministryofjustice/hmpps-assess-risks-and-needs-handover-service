package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response

import java.util.UUID

data class AssociationsResponse(
  val sanAssessmentId: UUID?,
  val sentencePlanId: UUID?,
)
