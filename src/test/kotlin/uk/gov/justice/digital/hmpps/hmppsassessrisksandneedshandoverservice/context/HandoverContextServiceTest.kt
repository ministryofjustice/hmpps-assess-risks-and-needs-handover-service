package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.GetHandoverContextResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService

class HandoverContextServiceTest {
  private lateinit var handoverContextService: HandoverContextService
  private val handoverContextRepository: HandoverContextRepository = mockk()

  @BeforeEach
  fun setUp() {
    handoverContextService = HandoverContextService(handoverContextRepository)
  }

  @Test
  fun `updateContext should return Success when context exists and update successful`() {
    val handoverSessionId = "testSessionId"
    val existingContext = TestUtils.createHandoverContext(handoverSessionId)
    val handoverRequest = TestUtils.createHandoverRequest()
    val updatedContext = existingContext.copy(
      principal = handoverRequest.principal,
      subject = handoverRequest.subject,
      assessmentContext = handoverRequest.assessmentContext,
      sentencePlanContext = handoverRequest.sentencePlanContext,
    )

    every { handoverContextRepository.findByHandoverSessionId(handoverSessionId) } returns existingContext
    every { handoverContextRepository.save(any()) } returns updatedContext

    val result = handoverContextService.updateContext(handoverSessionId, handoverRequest)

    assertTrue(result is GetHandoverContextResult.Success)
    assertEquals(updatedContext, (result as GetHandoverContextResult.Success).handoverContext)
    verify { handoverContextRepository.save(match { it == updatedContext }) }
  }

  @Test
  fun `getContext should return Success when context exists`() {
    val handoverSessionId = "testSessionId"
    val existingContext = TestUtils.createHandoverContext(handoverSessionId)

    every { handoverContextRepository.findByHandoverSessionId(handoverSessionId) } returns existingContext

    val result = handoverContextService.getContext(handoverSessionId)

    assertTrue(result is GetHandoverContextResult.Success)
    assertEquals(existingContext, (result as GetHandoverContextResult.Success).handoverContext)
    verify { handoverContextRepository.findByHandoverSessionId(any()) }
  }

  @Test
  fun `saveContext should save the given handover request`() {
    val handoverSessionId = "testSessionId"
    val handoverContext = TestUtils.createHandoverContext(handoverSessionId)
    val handoverRequest = TestUtils.createHandoverRequest()
    every { handoverContextRepository.save(any()) } returns handoverContext

    val result = handoverContextService.saveContext(handoverSessionId, handoverRequest)
    assertEquals(result, handoverContext)
    verify { handoverContextRepository.save(any()) }
  }

}
