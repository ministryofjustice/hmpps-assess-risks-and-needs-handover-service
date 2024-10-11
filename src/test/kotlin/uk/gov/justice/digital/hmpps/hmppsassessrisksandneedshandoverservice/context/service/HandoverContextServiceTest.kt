package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request.UpdateHandoverContextRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import java.util.UUID

class HandoverContextServiceTest {
  private lateinit var handoverContextService: HandoverContextService
  private val handoverContextRepository: HandoverContextRepository = mockk()

  @BeforeEach
  fun setUp() {
    handoverContextService = HandoverContextService(handoverContextRepository)
  }

  @Nested
  @DisplayName("updateContext")
  inner class UpdateContext {
    private lateinit var handoverSessionId: UUID
    private lateinit var updateHandoverContextRequest: UpdateHandoverContextRequest
    private lateinit var existingContext: HandoverContext
    private lateinit var updatedContext: HandoverContext

    @BeforeEach
    fun setup() {
      handoverSessionId = UUID.randomUUID()
      updateHandoverContextRequest = TestUtils.updateHandoverContextRequest()
      existingContext = TestUtils.createHandoverContext(handoverSessionId)
      updatedContext = existingContext.copy(
        principal = updateHandoverContextRequest.principal,
        subject = updateHandoverContextRequest.subject,
        assessmentContext = updateHandoverContextRequest.assessmentContext,
        sentencePlanContext = updateHandoverContextRequest.sentencePlanContext,
      )
    }

    @Test
    fun `should find original context from repository when using valid handover session id`() {
      every { handoverContextRepository.findByHandoverSessionId(handoverSessionId) } returns existingContext
      every { handoverContextRepository.save(any()) } returns updatedContext

      handoverContextService.updateContext(handoverSessionId, updateHandoverContextRequest)

      verify { handoverContextRepository.findByHandoverSessionId(match { it == handoverSessionId }) }
    }

    @Test
    fun `should return not found when using invalid handover session id`() {
      val invalidUUID = UUID.randomUUID()
      every { handoverContextRepository.findByHandoverSessionId(any()) } returns null

      val result = handoverContextService.updateContext(invalidUUID, updateHandoverContextRequest)

      verify { handoverContextRepository.findByHandoverSessionId(match { it == invalidUUID }) }
      assertEquals(result, GetHandoverContextResult.NotFound)
    }

    @Test
    fun `should return Success when context exists and update successful`() {
      every { handoverContextRepository.findByHandoverSessionId(handoverSessionId) } returns existingContext
      every { handoverContextRepository.save(any()) } returns updatedContext

      val result = handoverContextService.updateContext(handoverSessionId, updateHandoverContextRequest)

      assertTrue(result is GetHandoverContextResult.Success)
      assertEquals(updatedContext, (result as GetHandoverContextResult.Success).handoverContext)
      verify {
        handoverContextRepository.save(
          withArg {
            assertEquals(updatedContext.principal, it.principal)
            assertEquals(updatedContext.subject, it.subject)
            assertEquals(updatedContext.assessmentContext, it.assessmentContext)
            assertEquals(updatedContext.sentencePlanContext, it.sentencePlanContext)
            assertEquals(existingContext.createdAt, it.createdAt)
            assertEquals(existingContext.handoverSessionId, it.handoverSessionId)
          },
        )
      }
    }
  }

  @Nested
  @DisplayName("getContext")
  inner class GetContext {
    private lateinit var handoverSessionId: UUID
    private lateinit var existingContext: HandoverContext

    @BeforeEach
    fun setup() {
      handoverSessionId = UUID.randomUUID()
      existingContext = TestUtils.createHandoverContext(handoverSessionId)
    }

    @Test
    fun `should find original context from repository when using valid handover session id`() {
      every { handoverContextRepository.findByHandoverSessionId(handoverSessionId) } returns existingContext

      val result = handoverContextService.getContext(handoverSessionId)

      assertEquals(existingContext, (result as GetHandoverContextResult.Success).handoverContext)
      verify { handoverContextRepository.findByHandoverSessionId(match { it == handoverSessionId }) }
    }

    @Test
    fun `should return not found when using invalid handover session id`() {
      val invalidUUID = UUID.randomUUID()
      every { handoverContextRepository.findByHandoverSessionId(any()) } returns null

      val result = handoverContextService.getContext(invalidUUID)

      verify { handoverContextRepository.findByHandoverSessionId(match { it == invalidUUID }) }
      assertEquals(result, GetHandoverContextResult.NotFound)
    }
  }

  @Nested
  @DisplayName("saveContext")
  inner class SaveContext {
    private lateinit var handoverSessionId: UUID
    private lateinit var handoverContext: HandoverContext

    @BeforeEach
    fun setup() {
      handoverSessionId = UUID.randomUUID()
      handoverContext = TestUtils.createHandoverContext(handoverSessionId)
    }

    @Test
    fun `should save the given handover context`() {
      every { handoverContextRepository.save(any()) } returns handoverContext

      val result = handoverContextService.saveContext(handoverContext)
      assertEquals(result, handoverContext)
      verify { handoverContextRepository.save(match { it == handoverContext }) }
    }
  }
}
