package uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import uk.gov.justice.digital.hmpps.hmppshandoverservice.context.entity.HandoverPrincipal
import java.io.Serializable
import java.time.Instant
import java.util.UUID

enum class TokenStatus {
  USED,
  UNUSED,
}

@RedisHash("HandoverToken")
class HandoverToken(
  @Id var code: String = UUID.randomUUID().toString(),
  var tokenStatus: TokenStatus = TokenStatus.UNUSED,
  var createdAt: Instant = Instant.now(),
  var handoverSessionId: String,
  var principal: HandoverPrincipal,
) : Serializable
