package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

data class HandoverOasysRequest(
  @Schema(description = "Details of the practitioner/principal making the handover request")
  val user: HandoverPrincipal,

  @Schema(description = "Details of the PoP/subject of the handover request")
  val subjectDetails: SubjectDetails,

  val oasysAssessmentPk: String,

  val assessmentUUID: String?,

  val assessmentVersion: String?,
)
