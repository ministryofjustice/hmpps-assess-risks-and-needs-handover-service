package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.AuthorizationCodeGrantAuthorization
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.entity.AuthorizationGrantMapper
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.repository.AuthorizationGrantAuthorizationRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.UserAccess
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails
import java.security.Principal
import java.time.Instant
import java.util.UUID

class RedisOAuth2AuthorizationServiceTest {
  private companion object {
    const val TOKEN_INVALIDATED_METADATA_KEY = "metadata.token.invalidated"
  }

  private val registeredClientRepository: RegisteredClientRepository = mockk()
  private val authorizationGrantAuthorizationRepository: AuthorizationGrantAuthorizationRepository = mockk()
  private lateinit var service: RedisOAuth2AuthorizationService

  private val registeredClientId = "registered-client-id"

  private val registeredClient: RegisteredClient = RegisteredClient.withId(registeredClientId)
    .clientId("test-client")
    .clientSecret("secret")
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .redirectUri("http://localhost/callback")
    .scope("profile")
    .build()

  @BeforeEach
  fun setUp() {
    service = RedisOAuth2AuthorizationService(registeredClientRepository, authorizationGrantAuthorizationRepository)
  }

  private val handoverPrincipal = HandoverPrincipal(
    identifier = "test-user",
    displayName = "Test User",
    accessMode = UserAccess.READ_WRITE,
    planAccessMode = UserAccess.READ_ONLY,
    returnUrl = "http://oasys-ui:3000",
  )

  private val handoverAuthDetails = HandoverAuthDetails(
    handoverSessionId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    principal = handoverPrincipal,
  )

  private fun buildAuthorization(id: String = UUID.randomUUID().toString()): OAuth2Authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
    .id(id)
    .principalName("test-user")
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .build()

  private fun createEntity(
    id: String = UUID.randomUUID().toString(),
    authorizationCodeValue: String? = null,
    accessTokenValue: String? = null,
    state: String? = null,
  ): AuthorizationCodeGrantAuthorization {
    val now = Instant.now()
    val expiry = now.plusSeconds(300)

    val builder = OAuth2Authorization.withRegisteredClient(registeredClient)
      .id(id)
      .principalName("test-user")
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)

    if (state != null) {
      builder.attribute(OAuth2ParameterNames.STATE, state)
    }

    if (authorizationCodeValue != null) {
      builder.token(OAuth2AuthorizationCode(authorizationCodeValue, now, expiry))
    }

    if (authorizationCodeValue != null) {
      val authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("http://localhost/authorize")
        .clientId("test-client")
        .redirectUri("http://localhost/callback")
        .scopes(setOf("profile"))
        .additionalParameters(
          mapOf(
            PkceParameterNames.CODE_CHALLENGE to "challenge",
            PkceParameterNames.CODE_CHALLENGE_METHOD to "S256",
          ),
        )
        .build()
      val authentication = UsernamePasswordAuthenticationToken(
        handoverPrincipal.identifier,
        null,
        handoverPrincipal.accessMode.toAuthorities("SAN") + handoverPrincipal.planAccessMode.toAuthorities("PLAN"),
      )
      authentication.details = handoverAuthDetails

      builder.attribute(OAuth2AuthorizationRequest::class.java.name, authorizationRequest)
      builder.attribute(Principal::class.java.name, authentication)
    }

    if (accessTokenValue != null) {
      builder.token(
        OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessTokenValue, now, expiry, setOf("profile")),
      )
    }

    return AuthorizationGrantMapper.from(builder.build())
  }

  @Nested
  @DisplayName("save()")
  inner class Save {
    @Test
    fun `should save authorization to repository`() {
      val authorization = buildAuthorization()
      every { authorizationGrantAuthorizationRepository.save(any()) } returns mockk()

      service.save(authorization)

      verify { authorizationGrantAuthorizationRepository.save(any()) }
    }

    @Test
    fun `should overwrite existing authorization when id already exists`() {
      val authorizationId = UUID.randomUUID().toString()
      val authorization = buildAuthorization(id = authorizationId)
      every { authorizationGrantAuthorizationRepository.save(any()) } returns mockk()

      service.save(authorization)

      verify { authorizationGrantAuthorizationRepository.save(match { it.id == authorizationId }) }
    }

    @Test
    fun `should persist authorization attributes for code grant context`() {
      val entity = createEntity(authorizationCodeValue = "test-auth-code")

      assertNotNull(entity.authorizationCodeContext)
      assertNull(entity.attributes)
    }

    @Test
    fun `should delete consumed authorization when code is invalidated and no long lived tokens remain`() {
      val authorizationId = UUID.randomUUID().toString()
      val now = Instant.now()
      val expiry = now.plusSeconds(300)
      val authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
        .id(authorizationId)
        .principalName("test-user")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .token(OAuth2AuthorizationCode("test-auth-code", now, expiry)) { metadata ->
          metadata[TOKEN_INVALIDATED_METADATA_KEY] = true
        }
        .token(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-access-token", now, expiry, setOf("profile")))
        .build()
      every { authorizationGrantAuthorizationRepository.deleteById(authorizationId) } just Runs

      service.save(authorization)

      verify(exactly = 1) { authorizationGrantAuthorizationRepository.deleteById(authorizationId) }
      verify(exactly = 0) { authorizationGrantAuthorizationRepository.save(any()) }
    }
  }

  @Nested
  @DisplayName("remove()")
  inner class Remove {
    @Test
    fun `should delete authorization by id`() {
      val authorizationId = UUID.randomUUID().toString()
      val authorization = buildAuthorization(id = authorizationId)
      every { authorizationGrantAuthorizationRepository.deleteById(authorizationId) } just Runs

      service.remove(authorization)

      verify { authorizationGrantAuthorizationRepository.deleteById(authorizationId) }
    }
  }

  @Nested
  @DisplayName("findById()")
  inner class FindById {
    @Test
    fun `should return authorization when it exists`() {
      val id = UUID.randomUUID().toString()
      val entity = createEntity(id = id)
      every { authorizationGrantAuthorizationRepository.findByIdOrNull(id) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findById(id)

      assertEquals(id, result?.id)
      assertEquals(registeredClientId, result?.registeredClientId)
    }

    @Test
    fun `should return null when authorization does not exist`() {
      val id = UUID.randomUUID().toString()
      every { authorizationGrantAuthorizationRepository.findByIdOrNull(id) } returns null

      val result = service.findById(id)

      assertNull(result)
    }
  }

  @Nested
  @DisplayName("findByToken()")
  inner class FindByToken {
    @Test
    fun `should find by authorization code when token type is CODE`() {
      val code = "test-auth-code"
      val entity = createEntity(authorizationCodeValue = code)
      every { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))

      assertEquals(entity.id, result?.id)
    }

    @Test
    fun `should reconstruct authorization code context from stored attributes`() {
      val code = "test-auth-code"
      val entity = createEntity(authorizationCodeValue = code)
      every { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))
      val authorizationRequest = result?.getAttribute<OAuth2AuthorizationRequest>(OAuth2AuthorizationRequest::class.java.name)
      val authentication = result?.getAttribute<Authentication>(Principal::class.java.name)

      assertNotNull(authorizationRequest)
      assertEquals("test-client", authorizationRequest?.clientId)
      assertEquals("http://localhost/callback", authorizationRequest?.redirectUri)
      assertEquals(setOf("profile"), authorizationRequest?.scopes)
      assertEquals("challenge", authorizationRequest?.additionalParameters?.get(PkceParameterNames.CODE_CHALLENGE))
      assertEquals("S256", authorizationRequest?.additionalParameters?.get(PkceParameterNames.CODE_CHALLENGE_METHOD))
      assertNotNull(authentication)
      assertEquals("test-user", authentication?.name)
      assertEquals(setOf("SAN_READ", "SAN_WRITE", "PLAN_READ"), authentication?.authorities?.map { it.authority }?.toSet())
      assertEquals(handoverAuthDetails, authentication?.details)
    }

    @Test
    fun `should find by access token when token type is ACCESS_TOKEN`() {
      val token = "test-access-token"
      val entity = createEntity(accessTokenValue = token)
      every { authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)

      assertEquals(entity.id, result?.id)
    }

    @Test
    fun `should find by state when token type is STATE`() {
      val state = "test-state"
      val entity = createEntity(state = state)
      every { authorizationGrantAuthorizationRepository.findByState(state) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findByToken(state, OAuth2TokenType(OAuth2ParameterNames.STATE))

      assertEquals(entity.id, result?.id)
    }

    @Test
    fun `should return null when token type is unknown`() {
      val token = "test-token"

      val result = service.findByToken(token, OAuth2TokenType("unknown"))

      assertNull(result)
    }

    @Test
    fun `should return null when no token is found`() {
      val token = "nonexistent-token"
      every { authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token) } returns null

      val result = service.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)

      assertNull(result)
    }

    @Test
    fun `should try lookup methods sequentially when token type is null`() {
      val token = "test-token"
      val entity = createEntity(accessTokenValue = token)
      every {
        authorizationGrantAuthorizationRepository.findByStateOrAuthorizationCode_TokenValue(token, token)
      } returns null
      every { authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val result = service.findByToken(token, null)

      assertEquals(entity.id, result?.id)
    }

    @Test
    fun `should return cached result on duplicate findByToken call`() {
      val code = "test-auth-code"
      val entity = createEntity(authorizationCodeValue = code)
      every { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient

      val first = service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))
      val second = service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))

      assertEquals(first, second)
      verify(exactly = 1) { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) }
    }

    @Test
    fun `should cache null results`() {
      val token = "nonexistent"
      every { authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token) } returns null

      service.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
      service.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)

      verify(exactly = 1) { authorizationGrantAuthorizationRepository.findByAccessToken_TokenValue(token) }
    }
  }

  @Nested
  @DisplayName("caching")
  inner class Caching {
    @Test
    fun `should clear findByToken cache after save`() {
      val code = "test-auth-code"
      val entity = createEntity(authorizationCodeValue = code)
      every { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient
      every { authorizationGrantAuthorizationRepository.save(any()) } returns mockk()

      service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))
      service.save(buildAuthorization())
      service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))

      verify(exactly = 2) { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) }
    }

    @Test
    fun `should clear findByToken cache after remove`() {
      val code = "test-auth-code"
      val entity = createEntity(authorizationCodeValue = code)
      every { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient
      every { authorizationGrantAuthorizationRepository.deleteById(any()) } just Runs

      service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))
      service.remove(buildAuthorization())
      service.findByToken(code, OAuth2TokenType(OAuth2ParameterNames.CODE))

      verify(exactly = 2) { authorizationGrantAuthorizationRepository.findByAuthorizationCode_TokenValue(code) }
    }

    @Test
    fun `should clear findById cache after save`() {
      val id = UUID.randomUUID().toString()
      val entity = createEntity(id = id)
      every { authorizationGrantAuthorizationRepository.findByIdOrNull(id) } returns entity
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient
      every { authorizationGrantAuthorizationRepository.save(any()) } returns mockk()

      service.findById(id)
      service.save(buildAuthorization())
      service.findById(id)

      verify(exactly = 2) { authorizationGrantAuthorizationRepository.findByIdOrNull(id) }
    }

    @Test
    fun `should clear caches after deleting consumed authorization`() {
      val id = UUID.randomUUID().toString()
      val entity = createEntity(id = id, authorizationCodeValue = "test-auth-code")
      val now = Instant.now()
      val expiry = now.plusSeconds(300)
      val authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
        .id(id)
        .principalName("test-user")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .token(OAuth2AuthorizationCode("test-auth-code", now, expiry)) { metadata ->
          metadata[TOKEN_INVALIDATED_METADATA_KEY] = true
        }
        .token(OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test-access-token", now, expiry, setOf("profile")))
        .build()
      every { authorizationGrantAuthorizationRepository.findByIdOrNull(id) } returns entity andThen null
      every { registeredClientRepository.findById(registeredClientId) } returns registeredClient
      every { authorizationGrantAuthorizationRepository.deleteById(id) } just Runs

      val first = service.findById(id)
      service.save(authorization)
      val second = service.findById(id)

      assertFalse(first == null)
      assertNull(second)
      verify(exactly = 2) { authorizationGrantAuthorizationRepository.findByIdOrNull(id) }
    }
  }
}
