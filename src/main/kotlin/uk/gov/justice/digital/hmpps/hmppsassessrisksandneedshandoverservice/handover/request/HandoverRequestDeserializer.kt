package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails

class HandoverRequestDeserializer : JsonDeserializer<HandoverRequest>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): HandoverRequest {
    val node: JsonNode = parser.codec.readTree(parser)

    return if (
      node.has("subject") &&
      node.has("assessmentContext") &&
      node.has("principal")
    ) {
      HandoverRequest(
        subject = parser.codec.treeToValue(node.get("subject"), SubjectDetails::class.java),
        principal = parser.codec.treeToValue(node.get("principal"), HandoverPrincipal::class.java),
        assessmentContext = parser.codec.treeToValue(node.get("assessmentContext"), AssessmentContext::class.java),
      )
    } else if (
      node.has("oasysAssessmentPk") &&
      node.has("user") &&
      node.has("subjectDetails")
    ) {
      HandoverRequest(
        subject = parser.codec.treeToValue(node.get("subjectDetails"), SubjectDetails::class.java),
        principal = parser.codec.treeToValue(node.get("user"), HandoverPrincipal::class.java),
        assessmentContext = AssessmentContext(
          oasysAssessmentPk = node.get("oasysAssessmentPk").toString(),
          assessmentUUID = node.get("assessmentUUID")?.toString(),
          assessmentVersion = node.get("assessmentVersion")?.toString(),
        ),
      )
    } else {
      throw IllegalArgumentException("Invalid request")
    }
  }
}
