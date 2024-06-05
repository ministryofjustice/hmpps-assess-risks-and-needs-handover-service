package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Entity for caching authorization information to Redis using Repository
 */
@RedisHash("authorization")
data class Authorization(
  @Id var id: String? = null,
  var registeredClientId: String? = null,
  var principalName: String? = null,
  var authorizationGrantType: String? = null,
  var authorizedScopes: String? = null,
  var attributes: String? = null,
  @Indexed var state: String? = null,

  @Indexed var authorizationCodeValue: String? = null,
  var authorizationCodeIssuedAt: Instant? = null,
  var authorizationCodeExpiresAt: Instant? = null,
  var authorizationCodeMetadata: String? = null,

  @Indexed var accessTokenValue: String? = null,
  var accessTokenIssuedAt: Instant? = null,
  var accessTokenExpiresAt: Instant? = null,
  var accessTokenMetadata: String? = null,
  var accessTokenType: String? = null,
  var accessTokenScopes: String? = null,

  @Indexed var refreshTokenValue: String? = null,
  var refreshTokenIssuedAt: Instant? = null,
  var refreshTokenExpiresAt: Instant? = null,
  var refreshTokenMetadata: String? = null,

  @Indexed var oidcIdTokenValue: String? = null,
  var oidcIdTokenIssuedAt: Instant? = null,
  var oidcIdTokenExpiresAt: Instant? = null,
  var oidcIdTokenMetadata: String? = null,
  var oidcIdTokenClaims: String? = null,

  @Indexed var userCodeValue: String? = null,
  var userCodeIssuedAt: Instant? = null,
  var userCodeExpiresAt: Instant? = null,
  var userCodeMetadata: String? = null,

  @Indexed var deviceCodeValue: String? = null,
  var deviceCodeIssuedAt: Instant? = null,
  var deviceCodeExpiresAt: Instant? = null,
  var deviceCodeMetadata: String? = null,

  @TimeToLive(unit = TimeUnit.MINUTES) var timeout: Long = 30L,
) : Serializable {
  init {
    timeout = getTimeToLiveInMinutes()
  }

  private fun getTimeToLiveInMinutes(): Long {
    val fallbackTtl = 30L
    val expiresAtList = listOfNotNull(
      this.authorizationCodeExpiresAt,
      this.accessTokenExpiresAt,
      this.refreshTokenExpiresAt,
      this.oidcIdTokenExpiresAt,
      this.userCodeExpiresAt,
      this.deviceCodeExpiresAt,
    )

    return expiresAtList.maxOfOrNull { Duration.between(Instant.now(), it).toMinutes() }
      ?: fallbackTtl
  }
}
