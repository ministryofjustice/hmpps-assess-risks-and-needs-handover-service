package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.UseHandoverLinkResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import java.util.*
import kotlin.test.assertContains

class HandoverServiceTest {

  private lateinit var handoverService: HandoverService
  private val handoverTokenRepository: HandoverTokenRepository = mockk()
  private val handoverContextService: HandoverContextService = mockk()
  private val appConfiguration = mockk<AppConfiguration>(relaxed = true)
  private var handoverSessionId = "testSessionId"

  @BeforeEach
  fun setUp() {
    handoverService = HandoverService(handoverTokenRepository, handoverContextService, appConfiguration)
    handoverSessionId = UUID.randomUUID().toString()
  }

  @Nested
  @DisplayName("createHandover")
  inner class CreateHandover {
    private lateinit var handoverRequest: CreateHandoverLinkRequest
    private lateinit var handoverToken: HandoverToken
    private lateinit var handoverContext: HandoverContext

    @BeforeEach
    fun setUp() {
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
          assessmentVersion = handoverRequest.assessmentVersion,
        ),
        sentencePlanContext = SentencePlanContext(
          oasysAssessmentPk = handoverRequest.oasysAssessmentPk,
          planVersion = handoverRequest.planVersion,
        ),
      )
    }

    @Test
    fun `should save the handover context with correct properties`() {
      every { handoverContextService.saveContext(any()) } returns handoverContext
      every { handoverTokenRepository.save(any()) } returns handoverToken

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
    fun `should save the handover token with the correct properties`() {
      every { handoverContextService.saveContext(any()) } returns handoverContext
      every { handoverTokenRepository.save(any()) } returns handoverToken

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
    fun `should return a valid handover link`() {
      val domain = "handover-service"
      val endpoints = TestUtils.createEndPoint()
      every { handoverContextService.saveContext(any()) } returns handoverContext
      every { handoverTokenRepository.save(any()) } returns handoverToken
      every { appConfiguration.self.externalUrl } returns domain
      every { appConfiguration.self.endpoints } returns endpoints

      val result = handoverService.createHandover(handoverRequest, handoverSessionId)
      assertContains(result.handoverLink, "${domain}${endpoints.handover}")
      verify {
        handoverTokenRepository.save(
          withArg {
            assertEquals(it.code, result.handoverLink.substringAfterLast('/'))
          },
        )
      }
    }
  }

  @Nested
  @DisplayName("consumeAndExchangeHandover")
  inner class ConsumeAndExchangeHandover {
    @Test
    fun `should return authenticated token when valid token is used `() {
      val handoverToken = TestUtils.createHandoverToken(TokenStatus.UNUSED)
      val consumedToken = handoverToken.copy(
        tokenStatus = TokenStatus.USED,
      )

      every { handoverTokenRepository.findById(any()) } returns Optional.of(handoverToken)
      every { handoverTokenRepository.save(any()) } returns handoverToken

      val result = handoverService.consumeAndExchangeHandover(handoverToken.code)

      assertEquals(true, (result as UseHandoverLinkResult.Success).authenticationToken.isAuthenticated)
      verify { handoverTokenRepository.findById(any()) }
      verify { handoverTokenRepository.save(handoverToken) }
    }

    @Test
    fun `should not save token and return HandoverLinkAlreadyUsed when used token is used `() {
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
