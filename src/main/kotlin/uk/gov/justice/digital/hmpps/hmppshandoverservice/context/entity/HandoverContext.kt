package uk.gov.justice.digital.hmpps.hmppshandoverservice.context.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

enum class UserAccess(val value: String) {
  READ_ONLY("READ_ONLY"),
  READ_WRITE("READ_WRITE"),
  ;

  fun toAuthorities(): List<GrantedAuthority> {
    val commonAuthorities = listOf(
      SimpleGrantedAuthority("SCOPE_openid"),
      SimpleGrantedAuthority("SCOPE_profile"),
    )

    return when (this) {
      READ_ONLY -> commonAuthorities + listOf(SimpleGrantedAuthority("READ"))
      READ_WRITE -> commonAuthorities + listOf(SimpleGrantedAuthority("READ"), SimpleGrantedAuthority("WRITE"))
    }
  }
}

class HandoverPrincipal(
  val identifier: String = "",
  val displayName: String = "",
  val accessMode: UserAccess = UserAccess.READ_ONLY,
) : Serializable {
  override fun toString(): String {
    return identifier
  }
}

@RedisHash("HandoverContext")
data class HandoverContext(
  @Id val id: String = UUID.randomUUID().toString(),
  @Indexed var handoverSessionId: String,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  val principal: HandoverPrincipal,
  val subject: Any?,
  val assessmentContext: Any?,
  val sentencePlanContext: Any?,
) : Serializable
