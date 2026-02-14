package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import java.util.UUID

@Repository
interface HandoverTokenRepository : CrudRepository<HandoverToken, UUID>
