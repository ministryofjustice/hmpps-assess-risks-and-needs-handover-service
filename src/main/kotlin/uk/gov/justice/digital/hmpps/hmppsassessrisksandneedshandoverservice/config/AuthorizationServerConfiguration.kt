package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AuthorizationServerConfiguration(
  private val jwtConfiguration: JwtConfiguration,
) {
  @Bean
  @Order(1)
  @Throws(Exception::class)
  fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)

    http.getConfigurer(OAuth2AuthorizationServerConfigurer::class.java)
      .oidc { oidc ->
        oidc
          .providerConfigurationEndpoint(Customizer.withDefaults())
      }

    http
      .oauth2ResourceServer { resourceServer -> resourceServer.authenticationManagerResolver(jwtConfiguration.issuerAuthenticationManagerResolver()) }

    return http.build()
  }
}
