package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.redis.core.index.Indexed
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service.JpaOAuth2AuthorizationService
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

@RedisHash("authorization")
data class JpaAuthorization(
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

  private fun mapOAuth2AuthorizationCode(auth: OAuth2Authorization.Token<OAuth2AuthorizationCode>) {
    authorizationCodeValue = auth.token.tokenValue
    authorizationCodeIssuedAt = auth.token.issuedAt
    authorizationCodeExpiresAt = auth.token.expiresAt
    authorizationCodeMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
  }

  private fun mapOidcIdToken(auth: OAuth2Authorization.Token<OidcIdToken>) {
    oidcIdTokenValue = auth.token.tokenValue
    oidcIdTokenIssuedAt = auth.token.issuedAt
    oidcIdTokenExpiresAt = auth.token.expiresAt
    oidcIdTokenMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
    oidcIdTokenClaims = auth.claims?.run(JpaOAuth2AuthorizationService::writeMap)
  }

  private fun mapAccessToken(auth: OAuth2Authorization.Token<OAuth2AccessToken>) {
    accessTokenValue = auth.token.tokenValue
    accessTokenIssuedAt = auth.token.issuedAt
    accessTokenExpiresAt = auth.token.expiresAt
    accessTokenMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
    accessTokenScopes = auth.token.scopes?.joinToString(",")
  }

  private fun mapRefreshToken(auth: OAuth2Authorization.Token<OAuth2RefreshToken>) {
    refreshTokenValue = auth.token.tokenValue
    refreshTokenIssuedAt = auth.token.issuedAt
    refreshTokenExpiresAt = auth.token.expiresAt
    refreshTokenMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
  }

  private fun mapUserCode(auth: OAuth2Authorization.Token<OAuth2UserCode>) {
    userCodeValue = auth.token.tokenValue
    userCodeIssuedAt = auth.token.issuedAt
    userCodeExpiresAt = auth.token.expiresAt
    userCodeMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
  }

  private fun mapDeviceCode(auth: OAuth2Authorization.Token<OAuth2DeviceCode>) {
    deviceCodeValue = auth.token.tokenValue
    deviceCodeIssuedAt = auth.token.issuedAt
    deviceCodeExpiresAt = auth.token.expiresAt
    deviceCodeMetadata = JpaOAuth2AuthorizationService.writeMap(auth.metadata)
  }

  companion object {
    fun from(authorization: OAuth2Authorization): JpaAuthorization {
      val entity = JpaAuthorization()

      entity.id = authorization.id
      entity.registeredClientId = authorization.registeredClientId
      entity.principalName = authorization.principalName
      entity.authorizationGrantType = authorization.authorizationGrantType.value
      entity.authorizedScopes = authorization.authorizedScopes.joinToString(",")
      entity.attributes = JpaOAuth2AuthorizationService.writeMap(authorization.attributes)
      entity.state = authorization.getAttribute(OAuth2ParameterNames.STATE)

      authorization.getToken(OAuth2AuthorizationCode::class.java)?.let(entity::mapOAuth2AuthorizationCode)
      authorization.getToken(OidcIdToken::class.java)?.let(entity::mapOidcIdToken)
      authorization.getToken(OAuth2AccessToken::class.java)?.let(entity::mapAccessToken)
      authorization.getToken(OAuth2RefreshToken::class.java)?.let(entity::mapRefreshToken)
      authorization.getToken(OAuth2UserCode::class.java)?.let(entity::mapUserCode)
      authorization.getToken(OAuth2DeviceCode::class.java)?.let(entity::mapDeviceCode)

      return entity
    }
  }
}
