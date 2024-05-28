package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.HandoverRequest

@Service
class HandoverContextService(
  private val handoverContextRepository: HandoverContextRepository,
) {

  fun updateContext(handoverSessionId: String, handoverRequest: HandoverRequest): GetHandoverContextResult {
    return handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?.let { existingContext ->
        val updatedContext = existingContext.copy(
          principal = handoverRequest.principal,
          subject = handoverRequest.subject,
          assessmentContext = handoverRequest.assessmentContext,
          sentencePlanContext = handoverRequest.sentencePlanContext
        )
        handoverContextRepository.save(updatedContext)
        GetHandoverContextResult.Success(updatedContext)
      }
      ?: GetHandoverContextResult.HandoverContextNotFound
  }

  fun saveContext(handoverSessionId: String, handoverRequest: HandoverRequest): HandoverContext {
    return handoverContextRepository.save(
      HandoverContext(
        handoverSessionId = handoverSessionId,
        principal = handoverRequest.principal,
        subject = handoverRequest.subject,
        assessmentContext = handoverRequest.assessmentContext,
        sentencePlanContext = handoverRequest.sentencePlanContext,
      ),
    )
  }

  fun getContext(handoverSessionId: String): GetHandoverContextResult {
    return handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?.let { GetHandoverContextResult.Success(it) }
      ?: GetHandoverContextResult.HandoverContextNotFound
  }
}

sealed class GetHandoverContextResult {
  data class Success(val handoverContext: HandoverContext) : GetHandoverContextResult()
  data object HandoverContextNotFound : GetHandoverContextResult()
}
