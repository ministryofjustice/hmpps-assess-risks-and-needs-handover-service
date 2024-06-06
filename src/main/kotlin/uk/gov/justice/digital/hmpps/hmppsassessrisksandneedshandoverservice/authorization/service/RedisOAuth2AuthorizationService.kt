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
import kotlin.reflect.KClass

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
    return registeredClientRepository
      .findById(entity.registeredClientId)
      ?.let { entity.toObject(it, ::parseMap) }
      ?: throw DataRetrievalFailureException(
        "The RegisteredClient with id '${entity.registeredClientId}' was not found in the RegisteredClientRepository.",
      )
  }

  fun OAuth2Authorization.Token<OAuth2AuthorizationCode>.toEntity(entity: Authorization) {
    entity.authorizationCodeValue = token.tokenValue
    entity.authorizationCodeIssuedAt = token.issuedAt
    entity.authorizationCodeExpiresAt = token.expiresAt
    entity.authorizationCodeMetadata = writeMap(metadata)
  }

  fun OAuth2Authorization.Token<OidcIdToken>.toEntity(entity: Authorization) {
    entity.oidcIdTokenValue = token.tokenValue
    entity.oidcIdTokenIssuedAt = token.issuedAt
    entity.oidcIdTokenExpiresAt = token.expiresAt
    entity.oidcIdTokenMetadata = writeMap(metadata)
    entity.oidcIdTokenClaims = claims?.run(::writeMap)
  }

  private fun toEntity(authorization: OAuth2Authorization): Authorization {
    val entity = Authorization().apply {
      id = authorization.id
      registeredClientId = authorization.registeredClientId
      principalName = authorization.principalName
      authorizationGrantType = authorization.authorizationGrantType.value
      authorizedScopes = authorization.authorizedScopes.joinToString(",")
      attributes = writeMap(authorization.attributes)
      state = authorization.getAttribute(OAuth2ParameterNames.STATE)
    }

    with(authorization) {
      getToken(OAuth2AuthorizationCode::class.java)?.toEntity(entity)
      getToken(OidcIdToken::class.java)?.toEntity(entity)
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
