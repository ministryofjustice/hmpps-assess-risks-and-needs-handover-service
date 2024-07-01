package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

data class CreateHandoverLinkRequest(
  @Schema(description = "Details of the practitioner/principal making the handover request")
  val user: HandoverPrincipal,

  @Schema(description = "Details of the PoP/subject of the handover request")
  val subjectDetails: SubjectDetails,

  @Schema(description = "Assessment PK used for matching OASys records to assessment UUID/plan UUID")
  val oasysAssessmentPk: String,

  @Schema(description = "Version of the assessment")
  val assessmentVersion: Long?,

  @Schema(description = "Version of the sentence plan")
  val planVersion: Long?,
)
