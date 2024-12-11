package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service

import com.microsoft.applicationinsights.TelemetryClient
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import java.util.UUID

class TelemetryServiceTest {
  private val telemetryClient: TelemetryClient = mockk()
  private val telemetryService = TelemetryService(telemetryClient)
  private val handoverContext = HandoverContext(
    handoverSessionId = UUID.fromString("f27b1ce0-81b9-46ad-8566-92712d006489"),
    principal = HandoverPrincipal(
      identifier = "user-id",
    ),
    subject = SubjectDetails(
      crn = null,
      pnc = null,
      nomisId = null,
      givenName = "",
      familyName = "",
      dateOfBirth = null,
      gender = 1,
      location = Location.COMMUNITY,
      sexuallyMotivatedOffenceHistory = null,
    ),
    assessmentContext = AssessmentContext(
      oasysAssessmentPk = "PK123456",
      assessmentId = UUID.fromString("75de2afd-7f26-46c8-9368-2b121950e72f"),
      assessmentVersion = 0,
    ),
    sentencePlanContext = SentencePlanContext(
      oasysAssessmentPk = "PK123456",
      planId = UUID.fromString("1a2cf6c5-011e-4a31-92db-05a0d3c252e4"),
      planVersion = 1,
    ),
  )

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @ParameterizedTest
  @EnumSource(Event::class)
  fun `test event is tracked`(event: Event) {
    every { telemetryClient.trackEvent(any<String>(), any<Map<String, String>>(), null) } just Runs

    telemetryService.track(event, handoverContext)

    val expectedProperties = mapOf(
      "USER_ID" to "user-id",
      "HANDOVER_SESSION_ID" to "f27b1ce0-81b9-46ad-8566-92712d006489",
      "OASYS_PK" to "PK123456",
      "ASSESSMENT_ID" to "75de2afd-7f26-46c8-9368-2b121950e72f",
      "ASSESSMENT_VERSION" to "0",
      "SENTENCE_PLAN_ID" to "1a2cf6c5-011e-4a31-92db-05a0d3c252e4",
      "SENTENCE_PLAN_VERSION" to "1",
    )

    verify(exactly = 1) { telemetryClient.trackEvent(event.name, expectedProperties, null) }
  }
}
