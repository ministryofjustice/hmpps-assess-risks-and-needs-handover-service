package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2DeviceCode
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.core.OAuth2UserCode
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.Authorization
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository.AuthorizationRepository
import java.time.Instant

@Service
class RedisOAuth2AuthorizationService(
  private val registeredClientRepository: RegisteredClientRepository,
  private val oAuth2AuthorizationRepository: AuthorizationRepository,
) : OAuth2AuthorizationService {

  companion object {
    private val objectMapper: ObjectMapper = ObjectMapper()

    init {
      val classLoader = RedisOAuth2AuthorizationService::class.java.classLoader
      val securityModules = SecurityJackson2Modules.getModules(classLoader)
      objectMapper.registerModules(securityModules)
      objectMapper.registerModule(OAuth2AuthorizationServerJackson2Module())
    }
  }

  override fun save(authorization: OAuth2Authorization) {
    oAuth2AuthorizationRepository.findByIdOrNull(authorization.id)?.let {
      oAuth2AuthorizationRepository.deleteById(it.id)
    }

    oAuth2AuthorizationRepository.save(toEntity(authorization))
  }

  override fun remove(authorization: OAuth2Authorization) {
    Assert.notNull(authorization, "authorization cannot be null")
    oAuth2AuthorizationRepository.deleteById(authorization.id)
  }

  override fun findById(id: String): OAuth2Authorization? {
    Assert.hasText(id, "id cannot be empty")
    return oAuth2AuthorizationRepository.findByIdOrNull(id)?.let(::toObject)
  }

  override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
    Assert.hasText(token, "token cannot be empty")

    return when {
      tokenType == null -> {
        oAuth2AuthorizationRepository.findByState(token)
          ?: oAuth2AuthorizationRepository.findByAuthorizationCodeValue(token)
          ?: oAuth2AuthorizationRepository.findByAccessTokenValue(token)
          ?: oAuth2AuthorizationRepository.findByOidcIdTokenValue(token)
          ?: oAuth2AuthorizationRepository.findByRefreshTokenValue(token)
          ?: oAuth2AuthorizationRepository.findByUserCodeValue(token)
          ?: oAuth2AuthorizationRepository.findByDeviceCodeValue(token)
      }
      OAuth2ParameterNames.STATE == tokenType.value -> oAuth2AuthorizationRepository.findByState(token)
      OAuth2ParameterNames.CODE == tokenType.value -> oAuth2AuthorizationRepository.findByAuthorizationCodeValue(token)
      OAuth2TokenType.ACCESS_TOKEN == tokenType -> oAuth2AuthorizationRepository.findByAccessTokenValue(token)
      OidcParameterNames.ID_TOKEN == tokenType.value -> oAuth2AuthorizationRepository.findByOidcIdTokenValue(token)
      OAuth2TokenType.REFRESH_TOKEN == tokenType -> oAuth2AuthorizationRepository.findByRefreshTokenValue(token)
      OAuth2ParameterNames.USER_CODE == tokenType.value -> oAuth2AuthorizationRepository.findByUserCodeValue(token)
      OAuth2ParameterNames.DEVICE_CODE == tokenType.value -> oAuth2AuthorizationRepository.findByDeviceCodeValue(token)
      else -> null
    }?.let(::toObject)
  }

  private fun toObject(entity: Authorization): OAuth2Authorization {
    val registeredClient = this.registeredClientRepository.findById(entity.registeredClientId)
      ?: throw DataRetrievalFailureException(
        "The RegisteredClient with id '${entity.registeredClientId}' was not found in the RegisteredClientRepository.",
      )

    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
      .id(entity.id)
      .principalName(entity.principalName)
      .authorizationGrantType(entity.authorizationGrantType?.let { resolveAuthorizationGrantType(it) })
      .authorizedScopes(StringUtils.commaDelimitedListToSet(entity.authorizedScopes).toSet())
      .attributes { attributes -> attributes.putAll(parseMap(entity.attributes)) }

    entity.state?.let {
      builder.attribute(OAuth2ParameterNames.STATE, entity.state)
    }

    entity.authorizationCodeValue?.let {
      val authorizationCode = OAuth2AuthorizationCode(
        entity.authorizationCodeValue,
        entity.authorizationCodeIssuedAt,
        entity.authorizationCodeExpiresAt,
      )
      builder.token(authorizationCode) { metadata -> metadata.putAll(parseMap(entity.authorizationCodeMetadata)) }
    }

    entity.accessTokenValue?.let {
      val accessToken = OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        entity.accessTokenValue,
        entity.accessTokenIssuedAt,
        entity.accessTokenExpiresAt,
        StringUtils.commaDelimitedListToSet(entity.accessTokenScopes).toSet(),
      )
      builder.token(accessToken) { metadata -> metadata.putAll(parseMap(entity.accessTokenMetadata)) }
    }

    entity.refreshTokenValue?.let {
      val refreshToken = OAuth2RefreshToken(
        entity.refreshTokenValue,
        entity.refreshTokenIssuedAt,
        entity.refreshTokenExpiresAt,
      )
      builder.token(refreshToken) { metadata -> metadata.putAll(parseMap(entity.refreshTokenMetadata)) }
    }

    entity.oidcIdTokenValue?.let {
      val idToken = OidcIdToken(
        entity.oidcIdTokenValue,
        entity.oidcIdTokenIssuedAt,
        entity.oidcIdTokenExpiresAt,
        parseMap(entity.oidcIdTokenClaims),
      )
      builder.token(idToken) { metadata -> metadata.putAll(parseMap(entity.oidcIdTokenMetadata)) }
    }

    entity.userCodeValue?.let {
      val userCode = OAuth2UserCode(
        entity.userCodeValue,
        entity.userCodeIssuedAt,
        entity.userCodeExpiresAt,
      )
      builder.token(userCode) { metadata -> metadata.putAll(parseMap(entity.userCodeMetadata)) }
    }

    entity.deviceCodeValue?.let {
      val deviceCode = OAuth2DeviceCode(
        entity.deviceCodeValue,
        entity.deviceCodeIssuedAt,
        entity.deviceCodeExpiresAt,
      )
      builder.token(deviceCode) { metadata -> metadata.putAll(parseMap(entity.deviceCodeMetadata)) }
    }

    return builder.build()
  }

  private fun toEntity(authorization: OAuth2Authorization): Authorization {
    val entity = Authorization()
    entity.id = authorization.id
    entity.registeredClientId = authorization.registeredClientId
    entity.principalName = authorization.principalName
    entity.authorizationGrantType = authorization.authorizationGrantType.value
    entity.authorizedScopes = authorization.authorizedScopes.joinToString(",")
    entity.attributes = writeMap(authorization.attributes)
    entity.state = authorization.getAttribute(OAuth2ParameterNames.STATE)

    authorization.getToken(OAuth2AuthorizationCode::class.java)?.let {
      setTokenValues(
        it,
        { value -> entity.authorizationCodeValue = value },
        { issuedAt -> entity.authorizationCodeIssuedAt = issuedAt },
        { expiresAt -> entity.authorizationCodeExpiresAt = expiresAt },
        { metadata -> entity.authorizationCodeMetadata = metadata },
      )
    }

    authorization.getToken(OAuth2AccessToken::class.java)?.let {
      setTokenValues(
        it,
        { value -> entity.accessTokenValue = value },
        { issuedAt -> entity.accessTokenIssuedAt = issuedAt },
        { expiresAt -> entity.accessTokenExpiresAt = expiresAt },
        { metadata -> entity.accessTokenMetadata = metadata },
      )

      it.token.scopes?.let { scopes ->
        entity.accessTokenScopes = scopes.joinToString(",")
      }
    }

    authorization.getToken(OAuth2RefreshToken::class.java)?. let {
      setTokenValues(
        it,
        { value -> entity.refreshTokenValue = value },
        { issuedAt -> entity.refreshTokenIssuedAt = issuedAt },
        { expiresAt -> entity.refreshTokenExpiresAt = expiresAt },
        { metadata -> entity.refreshTokenMetadata = metadata },
      )
    }

    authorization.getToken(OidcIdToken::class.java)?. let { it ->
      setTokenValues(
        it,
        { value -> entity.oidcIdTokenValue = value },
        { issuedAt -> entity.oidcIdTokenIssuedAt = issuedAt },
        { expiresAt -> entity.oidcIdTokenExpiresAt = expiresAt },
        { metadata -> entity.oidcIdTokenMetadata = metadata },
      )

      entity.oidcIdTokenClaims = it.claims?.let(::writeMap)
    }

    authorization.getToken(OAuth2UserCode::class.java)?. let {
      setTokenValues(
        it,
        { value -> entity.userCodeValue = value },
        { issuedAt -> entity.userCodeIssuedAt = issuedAt },
        { expiresAt -> entity.userCodeExpiresAt = expiresAt },
        { metadata -> entity.userCodeMetadata = metadata },
      )
    }

    authorization.getToken(OAuth2DeviceCode::class.java)?.let {
      setTokenValues(
        it,
        { value -> entity.deviceCodeValue = value },
        { issuedAt -> entity.deviceCodeIssuedAt = issuedAt },
        { expiresAt -> entity.deviceCodeExpiresAt = expiresAt },
        { metadata -> entity.deviceCodeMetadata = metadata },
      )
    }

    return entity
  }

  private fun setTokenValues(
    token: OAuth2Authorization.Token<*>?,
    tokenValueConsumer: (String) -> Unit,
    issuedAtConsumer: (Instant) -> Unit,
    expiresAtConsumer: (Instant) -> Unit,
    metadataConsumer: (String) -> Unit,
  ) {
    token?.let {
      val oAuth2Token = it.token
      tokenValueConsumer(oAuth2Token.tokenValue)
      issuedAtConsumer(oAuth2Token.issuedAt)
      expiresAtConsumer(oAuth2Token.expiresAt)
      metadataConsumer(writeMap(it.metadata))
    }
  }

  private fun parseMap(data: String?): Map<String, Any> {
    return try {
      objectMapper.readValue(data, object : TypeReference<Map<String, Any>>() {})
    } catch (ex: Exception) {
      throw IllegalArgumentException(ex.message, ex)
    }
  }

  private fun writeMap(metadata: Map<String, Any>): String {
    return try {
      objectMapper.writeValueAsString(metadata)
    } catch (ex: Exception) {
      throw IllegalArgumentException(ex.message, ex)
    }
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
}
