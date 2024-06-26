package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

@JsonDeserialize(using = HandoverRequestDeserializer::class)
data class HandoverRequest(
  @Schema(description = "Details of the practitioner/principal making the handover request")
  val principal: HandoverPrincipal,

  @Schema(description = "Details of the PoP/subject of the handover request")
  val subject: SubjectDetails,

  @Schema(description = "Context data for use within a strength-and-needs assessment", nullable = true)
  val assessmentContext: AssessmentContext? = null,

  @Schema(description = "Context data for use within sentence-plan", nullable = true)
  val sentencePlanContext: SentencePlanContext? = null,
)
