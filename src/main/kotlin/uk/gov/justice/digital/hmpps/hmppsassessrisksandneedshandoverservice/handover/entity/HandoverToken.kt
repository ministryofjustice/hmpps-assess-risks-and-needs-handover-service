package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import java.time.Instant
import java.util.UUID

enum class TokenStatus {
  USED,
  UNUSED,
}

@RedisHash("HandoverToken", timeToLive = 600)
data class HandoverToken(
  @Id var code: UUID = UUID.randomUUID(),
  var tokenStatus: TokenStatus = TokenStatus.UNUSED,
  var createdAt: Instant = Instant.now(),
  var handoverSessionId: UUID,
  var principal: HandoverPrincipal,
)
