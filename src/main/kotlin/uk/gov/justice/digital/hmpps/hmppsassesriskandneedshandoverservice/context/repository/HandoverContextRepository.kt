package uk.gov.justice.digital.hmpps.hmppsassesriskandneedshandoverservice.context.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsassesriskandneedshandoverservice.context.entity.HandoverContext

@Repository
interface HandoverContextRepository : CrudRepository<HandoverContext, String?> {
  fun findByHandoverSessionId(handoverSessionId: String): HandoverContext?
}