package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

@ConfigurationProperties(prefix = "app")
class ClientsProperties {
  var clients: Map<String, ClientProperties> = mutableMapOf()
}

class ClientProperties {
  lateinit var secret: String
  lateinit var oauthRedirectUri: String
  lateinit var handoverRedirectUri: String
}

@Configuration
@EnableConfigurationProperties(ClientsProperties::class)
class ClientConfiguration(
  val clientsProperties: ClientsProperties,
) {

  @Bean
  fun registeredClientRepository(): InMemoryRegisteredClientRepository {
    val tokenSettings = TokenSettings.builder()
      .accessTokenTimeToLive(Duration.ofHours(2))
      .refreshTokenTimeToLive(Duration.ofDays(30))
      .build()

    val registeredClients = clientsProperties.clients.map { (clientId, clientProperties) ->
      RegisteredClient.withId(clientId)
        .clientId(clientId)
        .clientSecret("{noop}${clientProperties.secret}")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri(clientProperties.oauthRedirectUri)
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .tokenSettings(tokenSettings)
        .clientSettings(ClientSettings.builder().build())
        .build()
    }

    return InMemoryRegisteredClientRepository(registeredClients)
  }
}
