package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@RedisHash("authorization")
data class AuthorizationCodeGrantAuthorization(
  @Id var id: String? = null,
  var registeredClientId: String? = null,
  var principalName: String? = null,
  var authorizedScopes: Set<String> = emptySet(),
  var authorizationCodeContext: String? = null,
  var attributes: String? = null,
  @Indexed var state: String? = null,
  var authorizationCode: AuthorizationCode? = null,
  var accessToken: AccessToken? = null,
  @TimeToLive(unit = TimeUnit.MINUTES) var timeout: Long = 30L,
) : Serializable {
  init {
    updateTimeToLive()
  }

  fun updateTimeToLive(now: Instant = Instant.now()) {
    val fallbackTtl = 30L
    val expiresAt = listOfNotNull(
      authorizationCode?.expiresAt,
      accessToken?.expiresAt,
    )

    timeout = expiresAt.maxOfOrNull { Duration.between(now, it).toMinutes() }
      ?.coerceAtLeast(1L)
      ?: fallbackTtl
  }

  data class AuthorizationCode(
    @Indexed var tokenValue: String? = null,
    var issuedAt: Instant? = null,
    var expiresAt: Instant? = null,
    var metadata: String? = null,
  ) : Serializable

  data class AccessToken(
    @Indexed var tokenValue: String? = null,
    var issuedAt: Instant? = null,
    var expiresAt: Instant? = null,
    var metadata: String? = null,
    var tokenType: String? = null,
    var scopes: Set<String> = emptySet(),
  ) : Serializable
}
