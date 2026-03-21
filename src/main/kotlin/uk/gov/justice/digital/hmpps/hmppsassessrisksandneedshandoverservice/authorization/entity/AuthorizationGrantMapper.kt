package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity

import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.jackson.HandoverAuthDetailsMixin
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.jackson.HandoverPrincipalMixin
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails

object AuthorizationGrantMapper {
  private val compactObjectMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .build()

  private val objectMapper: JsonMapper = run {
    val classLoader = AuthorizationGrantMapper::class.java.classLoader

    JsonMapper.builder()
      .addModule(KotlinModule.Builder().build())
      .addModules(
        SecurityJacksonModules.getModules(
          classLoader,
          BasicPolymorphicTypeValidator
            .builder()
            .allowIfSubType(HandoverAuthDetails::class.java)
            .allowIfSubType(HandoverPrincipal::class.java),
        ),
      )
      .addModule(OAuth2AuthorizationServerJacksonModule())
      .addMixIn(HandoverAuthDetails::class.java, HandoverAuthDetailsMixin::class.java)
      .addMixIn(HandoverPrincipal::class.java, HandoverPrincipalMixin::class.java)
      .build()
  }

  fun from(authorization: OAuth2Authorization): AuthorizationCodeGrantAuthorization {
    require(authorization.authorizationGrantType == AuthorizationGrantType.AUTHORIZATION_CODE) {
      "Unsupported authorization grant type: ${authorization.authorizationGrantType?.value}"
    }

    val entity = AuthorizationCodeGrantAuthorization(
      id = authorization.id,
      registeredClientId = authorization.registeredClientId,
      principalName = authorization.principalName,
      authorizedScopes = authorization.authorizedScopes,
      state = authorization.getAttribute(OAuth2ParameterNames.STATE),
      authorizationCodeContext = null,
      attributes = writeMap(authorization.attributes),
      authorizationCode = authorization.getToken(OAuth2AuthorizationCode::class.java)?.let(::mapAuthorizationCode),
      accessToken = authorization.getToken(OAuth2AccessToken::class.java)?.let(::mapAccessToken),
    )

    entity.updateTimeToLive()

    return entity
  }

  fun toOAuth2Authorization(
    entity: AuthorizationCodeGrantAuthorization,
    registeredClient: RegisteredClient,
  ): OAuth2Authorization {
    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
      .id(entity.id)
      .principalName(entity.principalName)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizedScopes(entity.authorizedScopes)

    when {
      !entity.authorizationCodeContext.isNullOrBlank() -> {
        parseAuthorizationCodeContext(entity.authorizationCodeContext)
          ?.toAttributes(entity.principalName.orEmpty())
          ?.let { attributes -> builder.attributes { it.putAll(attributes) } }
      }
      !entity.attributes.isNullOrBlank() -> {
        builder.attributes { it.putAll(parseMap(entity.attributes)) }
      }
    }

    entity.authorizationCode?.let { authorizationCode ->
      builder.token(
        OAuth2AuthorizationCode(
          authorizationCode.tokenValue,
          authorizationCode.issuedAt,
          authorizationCode.expiresAt,
        ),
      ) { metadata ->
        metadata.putAll(parseMap(authorizationCode.metadata))
      }
    }

    entity.accessToken?.let { accessToken ->
      builder.token(
        OAuth2AccessToken(
          OAuth2AccessToken.TokenType.BEARER,
          accessToken.tokenValue,
          accessToken.issuedAt,
          accessToken.expiresAt,
          accessToken.scopes,
        ),
      ) { metadata ->
        metadata.putAll(parseMap(accessToken.metadata))
      }
    }

    return builder.build()
  }

  fun parseMap(data: String?): Map<String, Any> = try {
    if (data.isNullOrBlank()) {
      return emptyMap()
    }

    objectMapper.readValue(data, object : TypeReference<Map<String, Any>>() {})
  } catch (ex: Exception) {
    throw IllegalArgumentException(ex.message, ex)
  }

  fun writeMap(metadata: Map<String, Any>): String = try {
    objectMapper.writeValueAsString(metadata)
  } catch (ex: Exception) {
    throw IllegalArgumentException(ex.message, ex)
  }

  fun parseAuthorizationCodeContext(data: String?): AuthorizationCodeContext? = try {
    if (data.isNullOrBlank()) {
      return null
    }

    compactObjectMapper.readValue(data, AuthorizationCodeContext::class.java)
  } catch (ex: Exception) {
    throw IllegalArgumentException(ex.message, ex)
  }

  fun writeAuthorizationCodeContext(context: AuthorizationCodeContext): String = try {
    compactObjectMapper.writeValueAsString(context)
  } catch (ex: Exception) {
    throw IllegalArgumentException(ex.message, ex)
  }

  private fun mapAuthorizationCode(
    authorizationCode: OAuth2Authorization.Token<OAuth2AuthorizationCode>,
  ): AuthorizationCodeGrantAuthorization.AuthorizationCode = AuthorizationCodeGrantAuthorization.AuthorizationCode(
    tokenValue = authorizationCode.token.tokenValue,
    issuedAt = authorizationCode.token.issuedAt,
    expiresAt = authorizationCode.token.expiresAt,
    metadata = writeMap(authorizationCode.metadata),
  )

  private fun mapAccessToken(
  accessToken: OAuth2Authorization.Token<OAuth2AccessToken>,
  ): AuthorizationCodeGrantAuthorization.AccessToken = AuthorizationCodeGrantAuthorization.AccessToken(
    tokenValue = accessToken.token.tokenValue,
    issuedAt = accessToken.token.issuedAt,
    expiresAt = accessToken.token.expiresAt,
    metadata = writeMap(accessToken.metadata),
    tokenType = accessToken.token.tokenType.value,
    scopes = accessToken.token.scopes,
  )
}
