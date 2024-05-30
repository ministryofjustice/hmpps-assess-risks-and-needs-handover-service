package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.GetHandoverContextResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import kotlin.test.assertContains

class HandoverServiceTest {

  private lateinit var handoverService: HandoverService
  private val handoverTokenRepository: HandoverTokenRepository = mockk()
  private val handoverContextService: HandoverContextService = mockk()
  private val appConfiguration: AppConfiguration = mockk()
  val handoverSessionId = "testSessionId"
  @BeforeEach
  fun setUp() {
    handoverService = HandoverService(handoverTokenRepository, handoverContextService, appConfiguration)
  }


  @Test
  fun `create handover should save the given handover request and return the link `() {
    val handoverRequest = TestUtils.createHandoverRequest()
    val handoverToken = HandoverToken(
      handoverSessionId = handoverSessionId,
      principal = handoverRequest.principal,
    )
    val handoverContext = TestUtils.createHandoverContext(handoverSessionId)
    every { handoverContextService.saveContext(handoverSessionId, any()) } returns handoverContext
    every { handoverTokenRepository.save(any()) } returns handoverToken
    every { appConfiguration.self.externalUrl } returns "/handover_url"
    every { appConfiguration.self.endpoints } returns TestUtils.createEndPoint()
    val result = handoverService.createHandover(handoverRequest, handoverSessionId)

    assertEquals(result.handoverSessionId, handoverSessionId)
    assertContains(result.handoverLink, "/handover_url/handover_endpoint/")

    verify { handoverContextService.saveContext(handoverSessionId, any()) }
    verify { handoverTokenRepository.save(any()) }
  }
}