package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service

import io.mockk.Awaits
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Accommodation
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.CriminogenicNeedsData
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.GetHandoverContextResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.response.AssociationsResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service.CoordinatorService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.events.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service.TelemetryService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.util.*
import kotlin.test.assertContains
import kotlinx.coroutines.runBlocking
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.events.AuditEvent

class HandoverServiceTest {

  private lateinit var handoverService: HandoverService
  private lateinit var handoverSessionId: UUID
  private val handoverTokenRepository: HandoverTokenRepository = mockk()
  private val handoverContextService: HandoverContextService = mockk()
  private val telemetryService: TelemetryService = mockk()
  private val auditService: HmppsAuditService = mockk()
  private val coordinatorService: CoordinatorService = mockk()
  private val appConfiguration = mockk<AppConfiguration>(relaxed = true)

  @BeforeEach
  fun setUp() {
    handoverService = HandoverService(handoverTokenRepository, handoverContextService, coordinatorService, appConfiguration, telemetryService, auditService)
    handoverSessionId = UUID.randomUUID()
  }

  @Nested
  @DisplayName("createHandover")
  inner class CreateHandover {
    private lateinit var handoverRequest: CreateHandoverLinkRequest
    private lateinit var handoverToken: HandoverToken
    private lateinit var handoverContext: HandoverContext
    private lateinit var associations: AssociationsResponse

    @BeforeEach
    fun setUp() {
      clearAllMocks()

      associations = AssociationsResponse(
        sanAssessmentId = UUID.randomUUID(),
        sentencePlanId = UUID.randomUUID(),
      )
      handoverRequest = TestUtils.createHandoverRequest()
      handoverToken = HandoverToken(
        handoverSessionId = handoverSessionId,
        principal = handoverRequest.user,
      )
      handoverContext = HandoverContext(
        handoverSessionId = handoverSessionId,
        principal = handoverRequest.user,
        subject = handoverRequest.subjectDetails,
        assessmentContext = AssessmentContext(
          oasysAssessmentPk = handoverRequest.oasysAssessmentPk,
          assessmentId = associations.sanAssessmentId,
          assessmentVersion = handoverRequest.assessmentVersion,
        ),
        sentencePlanContext = SentencePlanContext(
          oasysAssessmentPk = handoverRequest.oasysAssessmentPk,
          planId = associations.sentencePlanId,
          planVersion = handoverRequest.sentencePlanVersion,
        ),
        criminogenicNeedsData = CriminogenicNeedsData(
          accommodation = Accommodation(
            accLinkedToHarm = "YES",
          ),
        ),
      )

      every { coordinatorService.getAssociations(any()) } returns associations
      every { handoverContextService.saveContext(any()) } returns handoverContext
      every { handoverTokenRepository.save(any()) } returns handoverToken
      every { telemetryService.track(any(), any()) } just Runs
      coEvery { auditService.publishEvent(what = any(), who = any(), service = any(), subjectId = any(), subjectType = any()) } just Awaits
    }

    @Test
    fun `should save the handover context with correct properties`() = runBlocking {
      handoverService.createHandover(handoverRequest, handoverSessionId)

      verify {
        handoverContextService.saveContext(
          withArg {
            assertEquals(handoverSessionId, it.handoverSessionId)
            assertEquals(handoverContext.subject, it.subject)
            assertEquals(handoverContext.principal, it.principal)
            assertEquals(handoverContext.assessmentContext, it.assessmentContext)
            assertEquals(handoverContext.sentencePlanContext, it.sentencePlanContext)
          },
        )
      }
    }

    @Test
    fun `should track a telemetry event`() = runBlocking {
      handoverService.createHandover(handoverRequest, handoverSessionId)

      verify(exactly = 1) {
        telemetryService.track(
          TelemetryEvent.ONE_TIME_LINK_CREATED,
          withArg {
            assertEquals(handoverSessionId, it.handoverSessionId)
            assertEquals(handoverContext.principal, it.principal)
          },
        )
      }
    }

    @Test
    fun `should track an audit event`() = runBlocking {
      handoverService.createHandover(handoverRequest, handoverSessionId)

      coVerify(exactly = 1) {
        auditService.publishEvent(
          what = AuditEvent.ONE_TIME_LINK_CREATED.name,
          subjectId = handoverContext.subject.crn,
          subjectType = "CRN",
          who = handoverContext.principal.identifier,
          service = "hmpps-assess-risks-and-needs-handover-service",
        )
      }
    }

    @Test
    fun `should save the handover token with the correct properties`() = runBlocking {
      handoverService.createHandover(handoverRequest, handoverSessionId)

      verify {
        handoverTokenRepository.save(
          withArg {
            assertEquals(handoverSessionId, it.handoverSessionId)
            assertEquals(handoverContext.principal, it.principal)
            assertEquals(TokenStatus.UNUSED, it.tokenStatus)
          },
        )
      }
    }

    @Test
    fun `should return a valid handover link`() = runBlocking {
      val domain = "handover-service"
      val endpoints = TestUtils.createEndPoint()
      every { appConfiguration.self.externalUrl } returns domain
      every { appConfiguration.self.endpoints } returns endpoints
      every { telemetryService.track(any(), any()) } just Runs

      val result = handoverService.createHandover(handoverRequest, handoverSessionId)
      assertContains(result.handoverLink, "${domain}${endpoints.handover}")
      verify {
        handoverTokenRepository.save(
          withArg {
            assertEquals(it.code.toString(), result.handoverLink.substringAfterLast('/'))
          },
        )
      }
    }
  }

  @Nested
  @DisplayName("consumeAndExchangeHandover")
  inner class ConsumeAndExchangeHandover {
    @Test
    fun `should return authenticated token when valid token is used `() = runBlocking {
      val handoverContext: HandoverContext = mockk()
      val handoverToken = TestUtils.createHandoverToken(TokenStatus.UNUSED)

      every { handoverTokenRepository.findById(any()) } returns Optional.of(handoverToken)
      every { handoverTokenRepository.save(any()) } returns handoverToken
      every { handoverContextService.getContext(any()) } returns GetHandoverContextResult.Success(handoverContext)
      every { telemetryService.track(any(), any()) } just Runs
      coEvery { auditService.publishEvent(what = any(), who = any(), service = any(), subjectId = any(), subjectType = any()) } just Awaits

      val result = handoverService.consumeAndExchangeHandover(handoverToken.code)

      assertEquals(true, (result as UseHandoverLinkResult.Success).authenticationToken.isAuthenticated)
      verify { handoverTokenRepository.findById(any()) }
      verify { handoverTokenRepository.save(handoverToken) }
      verify(exactly = 1) { telemetryService.track(TelemetryEvent.ONE_TIME_LINK_USED, handoverContext) }
      coVerify(exactly = 1) { auditService.publishEvent(
        what = AuditEvent.ONE_TIME_LINK_CREATED.name,
        subjectId = handoverContext.subject.crn,
        subjectType = "CRN",
        who = handoverContext.principal.identifier,
        service = "hmpps-assess-risks-and-needs-handover-service",
      ) }
    }

    @Test
    fun `should not save token and return HandoverLinkAlreadyUsed when used token is used `() = runBlocking {
      val handoverToken = TestUtils.createHandoverToken(TokenStatus.USED)

      every { handoverTokenRepository.findById(any()) } returns Optional.of(handoverToken)
      every { handoverTokenRepository.save(any()) } returns handoverToken

      val result = handoverService.consumeAndExchangeHandover(handoverToken.code)

      assertEquals(result, UseHandoverLinkResult.HandoverLinkAlreadyUsed)
      verify { handoverTokenRepository.findById(any()) wasNot Called }
      verify { handoverTokenRepository.save(any()) wasNot Called }
    }
  }
}
