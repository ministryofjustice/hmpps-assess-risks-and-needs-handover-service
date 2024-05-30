package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.GetHandoverContextResult
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service.HandoverContextService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.HandoverRequest
import java.time.LocalDateTime
import java.util.UUID


@ExtendWith(MockKExtension::class)
@DisplayName("Handover Context Service Tests")
class HandoverContextServiceTest {
  private val handoverContextRepository: HandoverContextRepository = mockk()
  private val handoverContextService = HandoverContextService(handoverContextRepository)
  private val createdAt = LocalDateTime.now()
  private val localDate = createdAt.toLocalDate()
  private val sessionId = UUID.randomUUID().toString()
  private val subject = SubjectDetails(
    givenName = "some name",
    crn = "crn",
    nomisId = "id",
    familyName = "xyz",
    dateOfBirth= localDate.minusYears(20),
    gender = 1,
    location = Location.PRISON,
    sexuallyMotivatedOffenceHistory = "history",
    pnc = "pnc",
  )
  private val assessmentContext = AssessmentContext(
    oasysAssessmentPk = "abc",
    assessmentUUID = "ok",
    assessmentVersion= "1.0",
  )
  private val sentencePlanContext = SentencePlanContext(
    oasysPk = "ok",
    assessmentVersion = "1.0",
  )
  private val handoverRequest = HandoverRequest(
    principal = HandoverPrincipal(),
    subject = subject,
    assessmentContext = assessmentContext,
    sentencePlanContext = sentencePlanContext,
  )
  private val handoverContext = HandoverContext(
    handoverSessionId = sessionId,
    principal = HandoverPrincipal(),
    assessmentContext = assessmentContext,
    sentencePlanContext = sentencePlanContext,
    subject = subject,
  )

  @Test
  fun `update handover context should respond with expected result`() {
    every { handoverContextRepository.findByHandoverSessionId(any()) } returns handoverContext
    every { handoverContextRepository.save(any()) } returns handoverContext
    val result = handoverContextService.updateContext("abc", handoverRequest)
    verify { handoverContextRepository.findByHandoverSessionId("abc") }
    verify { handoverContextRepository.save(any()) }
    val rSessionId = (result as GetHandoverContextResult.Success).handoverContext.id
    val rCreatedAt = result.handoverContext.createdAt
    val expectedResult = GetHandoverContextResult.Success(handoverContext=HandoverContext(id=rSessionId, handoverSessionId=sessionId, createdAt=rCreatedAt, subject=subject, assessmentContext=assessmentContext, principal = HandoverPrincipal(), sentencePlanContext=sentencePlanContext))
    Assertions.assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `save handover context should respond with expected result`() {
    every { handoverContextRepository.save(any()) } returns handoverContext
    handoverContextService.saveContext("abc", handoverRequest)
    verify { handoverContextRepository.save(any()) }
  }

  @Test
  fun `get handover context should respond with expected result`() {
    every { handoverContextRepository.findByHandoverSessionId(any()) } returns handoverContext
    val result = handoverContextService.getContext("abc")
    verify { handoverContextRepository.findByHandoverSessionId("abc") }
    val rSessionId = (result as GetHandoverContextResult.Success).handoverContext.id
    val rCreatedAt = result.handoverContext.createdAt
    val expectedResult = GetHandoverContextResult.Success(handoverContext=HandoverContext(id=rSessionId, handoverSessionId=sessionId, createdAt=rCreatedAt, subject=subject, assessmentContext=assessmentContext, principal = HandoverPrincipal(), sentencePlanContext=sentencePlanContext))
    Assertions.assertThat(result).isEqualTo(expectedResult)
  }
}