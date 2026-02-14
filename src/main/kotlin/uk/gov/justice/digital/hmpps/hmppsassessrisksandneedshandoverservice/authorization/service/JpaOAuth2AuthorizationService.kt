package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service

import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.JpaAuthorization
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.OAuth2AuthorizationBuilderExtension.from
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.jackson.HandoverAuthDetailsMixin
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.jackson.HandoverPrincipalMixin
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository.JpaAuthorizationRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails

@Service
class JpaOAuth2AuthorizationService(
  private val registeredClientRepository: RegisteredClientRepository,
  private val jpaAuthorizationRepository: JpaAuthorizationRepository,
) : OAuth2AuthorizationService {

  companion object {
    private val objectMapper: JsonMapper = run {
      val classLoader = JpaOAuth2AuthorizationService::class.java.classLoader

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

    fun parseMap(data: String?): Map<String, Any> = try {
      objectMapper.readValue(data, object : TypeReference<Map<String, Any>>() {})
    } catch (ex: Exception) {
      throw IllegalArgumentException(ex.message, ex)
    }

    fun writeMap(metadata: Map<String, Any>): String = try {
      objectMapper.writeValueAsString(metadata)
    } catch (ex: Exception) {
      throw IllegalArgumentException(ex.message, ex)
    }
  }

  override fun save(authorization: OAuth2Authorization) {
    jpaAuthorizationRepository.findByIdOrNull(authorization.id)?.let {
      it.id?.let { id ->
        jpaAuthorizationRepository.deleteById(id)
      }
    }

    jpaAuthorizationRepository.save(JpaAuthorization.from(authorization))
  }

  override fun remove(authorization: OAuth2Authorization) {
    Assert.notNull(authorization, "authorization cannot be null")
    jpaAuthorizationRepository.deleteById(authorization.id)
  }

  override fun findById(id: String): OAuth2Authorization? {
    Assert.hasText(id, "id cannot be empty")
    return jpaAuthorizationRepository.findByIdOrNull(id)?.let {
      val registeredClient = getRedisJpaAuthorizingClient(it)
      return OAuth2Authorization.withRegisteredClient(registeredClient).from(it).build()
    }
  }

  override fun findByToken(token: String, tokenType: OAuth2TokenType?): OAuth2Authorization? {
    Assert.hasText(token, "token cannot be empty")

    return when {
      tokenType == null -> {
        jpaAuthorizationRepository.findByState(token)
          ?: jpaAuthorizationRepository.findByAuthorizationCodeValue(token)
          ?: jpaAuthorizationRepository.findByAccessTokenValue(token)
          ?: jpaAuthorizationRepository.findByOidcIdTokenValue(token)
          ?: jpaAuthorizationRepository.findByRefreshTokenValue(token)
          ?: jpaAuthorizationRepository.findByUserCodeValue(token)
          ?: jpaAuthorizationRepository.findByDeviceCodeValue(token)
      }
      OAuth2ParameterNames.STATE == tokenType.value -> jpaAuthorizationRepository.findByState(token)
      OAuth2ParameterNames.CODE == tokenType.value -> jpaAuthorizationRepository.findByAuthorizationCodeValue(token)
      OAuth2TokenType.ACCESS_TOKEN == tokenType -> jpaAuthorizationRepository.findByAccessTokenValue(token)
      OidcParameterNames.ID_TOKEN == tokenType.value -> jpaAuthorizationRepository.findByOidcIdTokenValue(token)
      OAuth2TokenType.REFRESH_TOKEN == tokenType -> jpaAuthorizationRepository.findByRefreshTokenValue(token)
      OAuth2ParameterNames.USER_CODE == tokenType.value -> jpaAuthorizationRepository.findByUserCodeValue(token)
      OAuth2ParameterNames.DEVICE_CODE == tokenType.value -> jpaAuthorizationRepository.findByDeviceCodeValue(token)
      else -> null
    }?.let {
      val registeredClient = getRedisJpaAuthorizingClient(it)
      return OAuth2Authorization.withRegisteredClient(registeredClient).from(it).build()
    }
  }

  private fun getRedisJpaAuthorizingClient(entity: JpaAuthorization): RegisteredClient = this.registeredClientRepository.findById(entity.registeredClientId)
    ?: throw DataRetrievalFailureException(
      "The RegisteredClient with id '${entity.registeredClientId}' was not found in the RegisteredClientRepository.",
    )
}
