package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken

@Repository
interface HandoverTokenRepository : CrudRepository<HandoverToken, String?>
