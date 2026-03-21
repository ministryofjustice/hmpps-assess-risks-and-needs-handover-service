package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.util.StringUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service.JpaOAuth2AuthorizationService.Companion.parseAuthorizationCodeContext
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

  private fun OAuth2Authorization.Builder.authorizationGrantType(entity: JpaAuthorization): OAuth2Authorization.Builder = authorizationGrantType(
    entity.authorizationGrantType?.let {
      when (it) {
        AuthorizationGrantType.AUTHORIZATION_CODE.value -> AuthorizationGrantType.AUTHORIZATION_CODE
        AuthorizationGrantType.CLIENT_CREDENTIALS.value -> AuthorizationGrantType.CLIENT_CREDENTIALS
        else -> AuthorizationGrantType(it)
      }
    },
  )

  private fun OAuth2Authorization.Builder.authorizedScopes(entity: JpaAuthorization): OAuth2Authorization.Builder = authorizedScopes(StringUtils.commaDelimitedListToSet(entity.authorizedScopes).toSet())

  private fun OAuth2Authorization.Builder.attributes(entity: JpaAuthorization): OAuth2Authorization.Builder = when {
    !entity.authorizationCodeContext.isNullOrBlank() -> attributes { attributes ->
      parseAuthorizationCodeContext(entity.authorizationCodeContext)
        ?.toAttributes(entity.principalName.orEmpty())
        ?.let(attributes::putAll)
    }
    !entity.attributes.isNullOrBlank() -> attributes { attributes -> attributes.putAll(parseMap(entity.attributes)) }
    else -> this
  }

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
}
