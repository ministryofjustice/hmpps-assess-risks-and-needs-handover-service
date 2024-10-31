package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.CriminogenicNeedsData
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

data class CreateHandoverLinkRequest(
  @Schema(description = "Details of the practitioner/principal making the handover request")
  @field:Valid
  val user: HandoverPrincipal,

  @Schema(description = "Details of the PoP/subject of the handover request")
  @field:Valid
  val subjectDetails: SubjectDetails,

  @Schema(description = "Assessment PK used for matching OASys records to assessment UUID/plan UUID")
  @field:Size(min = 1, max = 15)
  val oasysAssessmentPk: String,

  @Schema(description = "Version of the assessment")
  val sanAssessmentVersion: Long? = null,

  @Schema(description = "Version of the sentence plan")
  val sentencePlanVersion: Long? = null,

  @Schema(description = "Criminogenic Needs Data")
  val criminogenicNeedsData: CriminogenicNeedsData? = null,
)
