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

@RedisHash("HandoverToken", timeToLive = 3600)
class HandoverToken(
  @Id var code: String = UUID.randomUUID().toString(),
  var tokenStatus: TokenStatus = TokenStatus.UNUSED,
  var createdAt: Instant = Instant.now(),
  var handoverSessionId: String,
  var principal: HandoverPrincipal,
)
