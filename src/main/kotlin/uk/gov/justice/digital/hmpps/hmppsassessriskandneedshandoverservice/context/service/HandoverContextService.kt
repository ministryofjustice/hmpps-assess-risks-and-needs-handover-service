package uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.handover.request.HandoverRequest

@Service
class HandoverContextService(
  private val handoverContextRepository: HandoverContextRepository,
) {

  fun updateContext(handoverSessionId: String, handoverRequest: HandoverRequest): HandoverContext {
    handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?: throw NoSuchElementException("No handover context found for session ID $handoverSessionId")

    // TODO: Maybe worth stopping them from updating the principal?
    return saveContext(handoverSessionId, handoverRequest)
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

  fun getContext(handoverSessionId: String): HandoverContext {
    return handoverContextRepository.findByHandoverSessionId(handoverSessionId)
      ?: throw NoSuchElementException("No handover context found for session ID $handoverSessionId")
  }
}
