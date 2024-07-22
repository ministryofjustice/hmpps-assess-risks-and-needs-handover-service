package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import java.util.UUID

@Repository
interface HandoverContextRepository : CrudRepository<HandoverContext, UUID?> {
  fun findByHandoverSessionId(handoverSessionId: UUID): HandoverContext?
}
