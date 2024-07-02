package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request

import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

data class UpdateHandoverContextRequest(
  val principal: HandoverPrincipal,
  val subject: SubjectDetails,
  val assessmentContext: AssessmentContext?,
  val sentencePlanContext: SentencePlanContext?,
)
