package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service

import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.AuthorizationCodeGrantAuthorization
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.AuthorizationGrantMapper
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository.AuthorizationGrantAuthorizationRepository

@Service
class RedisOAuth2AuthorizationService(
  private val registeredClientRepository: RegisteredClientRepository,
  private val authorizationGrantAuthorizationRepository: AuthorizationGrantAuthorizationRepository,
) : OAuth2AuthorizationService {

  // Thread-local cache avoids duplicate Redis lookups + Jackson deserialization
  // within a single request (e.g., CodeVerifierAuthenticator and
  // OAuth2AuthorizationCodeAuthenticationProvider both call findByToken with the same code)
  private val tokenCache = ThreadLocal.withInitial { mutableMapOf<String, OAuth2Authorization?>() }
  private val idCache = ThreadLocal.withInitial { mutableMapOf<String, OAuth2Authorization?>() }

  companion object {
    private const val TOKEN_INVALIDATED_METADATA_KEY = "metadata.token.invalidated"
  }

  override fun save(authorization: OAuth2Authorization) {
    if (shouldDeleteConsumedAuthorization(authorization)) {
      authorizationGrantAuthorizationRepository.deleteById(authorization.id)
    } else {
      authorizationGrantAuthorizationRepository.save(AuthorizationGrantMapper.from(authorization))
    }

    clearCache()
  }

  override fun remove(authorization: OAuth2Authorization) {
    Assert.notNull(authorization, "authorization cannot be null")
    authorizationGrantAuthorizationRepository.deleteById(authorization.id)
    clearCache()
  }

  override fun findById(id: String): OAuth2Authorization? {
    Assert.hasText(id, "id cannot be empty")
    val cache = idCache.get()

    if (cache.containsKey(id)) {
      return cache[id]
    }

    return authorizationGrantAuthorizationRepository.findByIdOrNull(id)
      ?.toOAuth2Authorization()
      .also { cache[id] = it }
  }

  override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
    Assert.hasText(token, "token cannot be empty")
    val cacheKey = "$token:${tokenType?.value}"
    val cache = tokenCache.get()

    if (cache.containsKey(cacheKey)) {
      return cache[cacheKey]
    }

    return lookupByToken(token, tokenType)
      ?.toOAuth2Authorization()
      .also { cache[cacheKey] = it }
  }

  private fun lookupByToken(token: String, tokenType: OAuth2TokenType?): AuthorizationCodeGrantAuthorization? = when {
    tokenType == null -> {
      authorizationGrantAuthorizationRepository.findByStateOrAuthorizationCode_TokenValue(token, token)
        ?: authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token)
    }
    OAuth2ParameterNames.STATE == tokenType.value -> authorizationGrantAuthorizationRepository.findByState(token)
    OAuth2ParameterNames.CODE == tokenType.value -> authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(token)
    OAuth2TokenType.ACCESS_TOKEN == tokenType -> authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token)
    else -> null
  }

  private fun AuthorizationCodeGrantAuthorization.toOAuth2Authorization(): OAuth2Authorization {
    val registeredClient = registeredClientRepository.findById(registeredClientId)
      ?: throw DataRetrievalFailureException(
        "The RegisteredClient with id '$registeredClientId' was not found in the RegisteredClientRepository.",
      )

    return AuthorizationGrantMapper.toOAuth2Authorization(this, registeredClient)
  }

  private fun clearCache() {
    tokenCache.remove()
    idCache.remove()
  }

  private fun shouldDeleteConsumedAuthorization(authorization: OAuth2Authorization): Boolean {
    if (authorization.authorizationGrantType != AuthorizationGrantType.AUTHORIZATION_CODE) {
      return false
    }

    val authorizationCode = authorization.getToken(OAuth2AuthorizationCode::class.java) ?: return false

    return authorizationCode.metadata[TOKEN_INVALIDATED_METADATA_KEY] == true
  }
}
