package uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice.context.entity.HandoverContext

@Repository
interface HandoverContextRepository : CrudRepository<HandoverContext, String?> {
  fun findByHandoverSessionId(handoverSessionId: String): HandoverContext?
}
