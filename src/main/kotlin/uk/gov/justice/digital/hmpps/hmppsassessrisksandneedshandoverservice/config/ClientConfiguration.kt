package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
class ClientsProperties {
  var client: Map<String, Any> = mutableMapOf()
}

@Configuration
@EnableConfigurationProperties(ClientsProperties::class)
class ClientConfiguration(
  val clientsProperties: ClientsProperties,
  val clientRepository: RegisteredClientRepository,
) {

  /* A little bit of a hack to set custom token settings for
   * each of the different registered clients in the application.yaml
   */
  fun updateClientTokenSettings(clientsProperties: ClientsProperties) {
    val tokenSettings = TokenSettings.builder()
      .accessTokenTimeToLive(Duration.ofHours(2))
      .refreshTokenTimeToLive(Duration.ofDays(30))
      .build()

    clientsProperties.client.keys.toList().forEach { client ->
      val clientObject = clientRepository.findByClientId(client) ?: throw NoSuchElementException()
      clientRepository.save(
        RegisteredClient.withId(client)
          .clientId(clientObject.clientId)
          .clientSecret(clientObject.clientSecret)
          .clientName(clientObject.clientName)
          .clientAuthenticationMethods { methods -> methods.addAll(clientObject.clientAuthenticationMethods) }
          .authorizationGrantTypes { grantTypes -> grantTypes.addAll(clientObject.authorizationGrantTypes) }
          .redirectUris { uris -> uris.addAll(clientObject.redirectUris) }
          .scopes { scopes -> scopes.addAll(clientObject.scopes) }
          .tokenSettings(tokenSettings)
          .clientSettings(clientObject.clientSettings)
          .build(),
      )
    }
  }

  @PostConstruct
  fun init() {
    this.updateClientTokenSettings(clientsProperties)
  }
}
