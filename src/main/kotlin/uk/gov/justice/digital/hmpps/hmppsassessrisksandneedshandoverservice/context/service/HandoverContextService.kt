package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request.UpdateHandoverContextRequest

@Service
class HandoverContextService(
  private val handoverContextRepository: HandoverContextRepository,
) {

  fun updateContext(handoverSessionId: String, handoverContext: UpdateHandoverContextRequest): GetHandoverContextResult {
    return handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?.let {
        val updatedContext = it.copy(
          principal = handoverContext.principal,
          subject = handoverContext.subject,
          assessmentContext = handoverContext.assessmentContext,
          sentencePlanContext = handoverContext.sentencePlanContext,
        )
        handoverContextRepository.save(updatedContext)
        GetHandoverContextResult.Success(updatedContext)
      }
      ?: GetHandoverContextResult.NotFound
  }

  fun saveContext(handoverContext: HandoverContext): HandoverContext {
    return handoverContextRepository.save(handoverContext)
  }

  fun getContext(handoverSessionId: String): GetHandoverContextResult {
    return handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?.let { GetHandoverContextResult.Success(it) }
      ?: GetHandoverContextResult.NotFound
  }
}

sealed class GetHandoverContextResult {
  data class Success(val handoverContext: HandoverContext) : GetHandoverContextResult()
  data object NotFound : GetHandoverContextResult()
}
