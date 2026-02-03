package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity

import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import java.io.Serializable
import java.util.UUID

data class HandoverAuthDetails(
  val handoverSessionId: UUID,
  val principal: HandoverPrincipal,
) : Serializable {
  companion object {
    private const val serialVersionUID: Long = 1L
  }
}
