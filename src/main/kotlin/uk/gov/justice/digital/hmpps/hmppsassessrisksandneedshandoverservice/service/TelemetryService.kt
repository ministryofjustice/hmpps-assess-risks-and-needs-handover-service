package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext

enum class Event {
  ONE_TIME_LINK_CREATED,
  ONE_TIME_LINK_USED,
}

@Service
class TelemetryService(
  val client: TelemetryClient,
) {
  fun track(event: Event, handoverContext: HandoverContext) = client.trackEvent(
    event.name,
    mapOf(
      "USER_ID" to handoverContext.principal.identifier,
      "HANDOVER_SESSION_ID" to handoverContext.handoverSessionId.toString(),
      "OASYS_PK" to (handoverContext.assessmentContext?.oasysAssessmentPk ?: ""),
      "ASSESSMENT_ID" to (handoverContext.assessmentContext?.assessmentId?.toString() ?: ""),
      "ASSESSMENT_VERSION" to (handoverContext.assessmentContext?.assessmentVersion?.toString() ?: ""),
      "SENTENCE_PLAN_ID" to (handoverContext.sentencePlanContext?.planId?.toString() ?: ""),
      "SENTENCE_PLAN_VERSION" to (handoverContext.sentencePlanContext?.planVersion?.toString() ?: ""),
    ),
    null,
  )
}
