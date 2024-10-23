package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration

@Configuration
class ClientConfiguration(
  private val appConfiguration: AppConfiguration,
) {

  @Bean
  fun registeredClientRepository(): InMemoryRegisteredClientRepository {
    val tokenSettings = TokenSettings.builder()
      .accessTokenTimeToLive(Duration.ofDays(1L))
      .refreshTokenTimeToLive(Duration.ofDays(2L))
      .build()

    val registeredClients = appConfiguration.clients
      .filter { (clientId, clientProperties) ->
        clientProperties.oauthRedirectUris.all { isUriValid(it) }
          .also { clientIsValid ->
            if (!clientIsValid) log.info("Client $clientId will be ignored due to a misconfigured oauth-redirect-uri.")
          }
      }
      .map { (clientId, clientProperties) ->
        val clientBuilder = RegisteredClient.withId(clientId)
          .clientId(clientId)
          .clientSecret("{noop}${clientProperties.secret}")
          .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
          .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
          .scope(OidcScopes.OPENID)
          .scope(OidcScopes.PROFILE)
          .tokenSettings(tokenSettings)
          .clientSettings(ClientSettings.builder().build())

        clientProperties.oauthRedirectUris.forEach { uri ->
          clientBuilder.redirectUri(uri)
        }

        clientBuilder.build()
      }

    return InMemoryRegisteredClientRepository(registeredClients)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun isUriValid(uri: String): Boolean {
      return try {
        URI(uri).fragment == null
      } catch (_: URISyntaxException) {
        false
      }
    }
  }
}
