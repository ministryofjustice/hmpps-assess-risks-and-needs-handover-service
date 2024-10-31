package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.CriminogenicNeedsData
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

data class UpdateHandoverContextRequest(
  @Schema(description = "Details of the practitioner/principal making the handover request")
  @field:Valid
  val principal: HandoverPrincipal,

  @Schema(description = "Details of the PoP/subject of the handover request")
  @field:Valid
  val subject: SubjectDetails,

  @Schema(description = "Assessment context details")
  @field:Valid
  val assessmentContext: AssessmentContext?,

  @Schema(description = "Sentence plan context details")
  @field:Valid
  val sentencePlanContext: SentencePlanContext?,

  @Schema(description = "Criminogenic Needs Scores")
  @field:Valid
  val criminogenicNeedsData: CriminogenicNeedsData? = null,

)
