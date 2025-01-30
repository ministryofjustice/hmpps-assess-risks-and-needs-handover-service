package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.util.StringUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service.JpaOAuth2AuthorizationService.Companion.parseMap

object OAuth2AuthorizationBuilderExtension {
  fun OAuth2Authorization.Builder.from(entity: JpaAuthorization): OAuth2Authorization.Builder = this
    .id(entity.id)
    .principalName(entity.principalName)
    .authorizationGrantType(entity)
    .authorizedScopes(entity)
    .attributes(entity)
    .buildAuthorizationCode(entity)
    .buildAccessToken(entity)
    .buildRefreshToken(entity)
    .buildOidcToken(entity)
    .buildUserCode(entity)
    .buildDeviceCode(entity)

  private fun OAuth2Authorization.Builder.authorizationGrantType(entity: JpaAuthorization): OAuth2Authorization.Builder = authorizationGrantType(
    entity.authorizationGrantType?.let {
      when (it) {
        AuthorizationGrantType.AUTHORIZATION_CODE.value -> AuthorizationGrantType.AUTHORIZATION_CODE
        AuthorizationGrantType.CLIENT_CREDENTIALS.value -> AuthorizationGrantType.CLIENT_CREDENTIALS
        AuthorizationGrantType.REFRESH_TOKEN.value -> AuthorizationGrantType.REFRESH_TOKEN
        AuthorizationGrantType.DEVICE_CODE.value -> AuthorizationGrantType.DEVICE_CODE
        else -> AuthorizationGrantType(it)
      }
    },
  )

  private fun OAuth2Authorization.Builder.authorizedScopes(entity: JpaAuthorization): OAuth2Authorization.Builder = authorizedScopes(StringUtils.commaDelimitedListToSet(entity.authorizedScopes).toSet())

  private fun OAuth2Authorization.Builder.attributes(entity: JpaAuthorization): OAuth2Authorization.Builder = attributes { attributes -> attributes.putAll(parseMap(entity.attributes)) }

  private fun OAuth2Authorization.Builder.buildAuthorizationCode(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.authorizationCodeValue == null) return this

    return this.token(
      OAuth2AuthorizationCode(
        entity.authorizationCodeValue,
        entity.authorizationCodeIssuedAt,
        entity.authorizationCodeExpiresAt,
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.authorizationCodeMetadata)) }
  }

  private fun OAuth2Authorization.Builder.buildAccessToken(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.accessTokenValue == null) return this

    return this.token(
      OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        entity.accessTokenValue,
        entity.accessTokenIssuedAt,
        entity.accessTokenExpiresAt,
        StringUtils.commaDelimitedListToSet(entity.accessTokenScopes).toSet(),
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.accessTokenMetadata)) }
  }

  private fun OAuth2Authorization.Builder.buildRefreshToken(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.refreshTokenValue == null) return this

    return this.token(
      OAuth2RefreshToken(
        entity.refreshTokenValue,
        entity.refreshTokenIssuedAt,
        entity.refreshTokenExpiresAt,
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.refreshTokenMetadata)) }
  }

  private fun OAuth2Authorization.Builder.buildOidcToken(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.oidcIdTokenValue == null) return this

    return this.token(
      OidcIdToken(
        entity.oidcIdTokenValue,
        entity.oidcIdTokenIssuedAt,
        entity.oidcIdTokenExpiresAt,
        parseMap(entity.oidcIdTokenClaims),
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.oidcIdTokenMetadata)) }
  }

  private fun OAuth2Authorization.Builder.buildUserCode(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.userCodeValue == null) return this

    return this.token(
      OAuth2UserCode(
        entity.userCodeValue,
        entity.userCodeIssuedAt,
        entity.userCodeExpiresAt,
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.userCodeMetadata)) }
  }

  private fun OAuth2Authorization.Builder.buildDeviceCode(entity: JpaAuthorization): OAuth2Authorization.Builder {
    if (entity.deviceCodeValue == null) return this

    return this.token(
      OAuth2DeviceCode(
        entity.deviceCodeValue,
        entity.deviceCodeIssuedAt,
        entity.deviceCodeExpiresAt,
      ),
    ) { metadata -> metadata.putAll(parseMap(entity.deviceCodeMetadata)) }
  }
}
