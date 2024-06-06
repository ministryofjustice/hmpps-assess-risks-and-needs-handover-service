package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Builder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.util.StringUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service.RedisOAuth2AuthorizationService
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

typealias ParseMap = (String?) -> Map<String, Any>

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

  fun toObject(registeredClient: RegisteredClient, parseMap: ParseMap): OAuth2Authorization {
    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
      .id(id)
      .principalName(principalName)
      .authorizationGrantType(authorizationGrantType?.let { resolveAuthorizationGrantType(it) })
      .authorizedScopes(StringUtils.commaDelimitedListToSet(authorizedScopes).toSet())
      .attributes { attr -> attr.putAll(parseMap(attributes)) }

    setOf(
      ::setOAuth2AuthorizationCode,
      ::setOAuth2AccessToken,
    ).forEach { it.call(builder, parseMap) }

    return builder.build()
  }

  private fun resolveAuthorizationGrantType(authorizationGrantType: String): AuthorizationGrantType {
    return when (authorizationGrantType) {
      AuthorizationGrantType.AUTHORIZATION_CODE.value -> AuthorizationGrantType.AUTHORIZATION_CODE
      AuthorizationGrantType.CLIENT_CREDENTIALS.value -> AuthorizationGrantType.CLIENT_CREDENTIALS
      AuthorizationGrantType.REFRESH_TOKEN.value -> AuthorizationGrantType.REFRESH_TOKEN
      AuthorizationGrantType.DEVICE_CODE.value -> AuthorizationGrantType.DEVICE_CODE
      else -> AuthorizationGrantType(authorizationGrantType)
    }
  }

  private fun setOAuth2AuthorizationCode(builder: Builder, parseMap: ParseMap) {
    authorizationCodeValue ?: return
    OAuth2AuthorizationCode(
      authorizationCodeValue,
      authorizationCodeIssuedAt,
      authorizationCodeExpiresAt,
    ).let { builder.token(it) { metadata -> metadata.putAll(parseMap(authorizationCodeMetadata))}}
  }

  private fun setOAuth2AccessToken(builder: Builder, parseMap: ParseMap) {
    accessTokenValue ?: return
    OAuth2AccessToken(
      OAuth2AccessToken.TokenType.BEARER,
      accessTokenValue,
      accessTokenIssuedAt,
      accessTokenExpiresAt,
      StringUtils.commaDelimitedListToSet(accessTokenScopes).toSet(),
    ).let { builder.token(it) { metadata -> metadata.putAll(parseMap(accessTokenMetadata))}}
  }
}
