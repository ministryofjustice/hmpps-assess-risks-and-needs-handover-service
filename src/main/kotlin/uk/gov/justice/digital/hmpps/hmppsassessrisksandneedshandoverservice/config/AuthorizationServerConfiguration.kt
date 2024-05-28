package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain

@Configuration
class AuthorizationServerConfiguration {

  @Bean
  @Order(1)
  @Throws(Exception::class)
  fun denyUnusedOauth2EndpointsSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    val urls = arrayOf(
      // Device Authorization Flow
      "/oauth2/device_authorization",
      // OAuth2 Consent Flow
      "/oauth2/consent",
      "/oauth2/decision",
      // OpenID Connect Logout
      "/connect/logout",
      // OpenID Client Registration
      "/connect/register",
    )

    http
      .securityMatcher { e -> urls.any { url -> url == e.requestURI } }
      .authorizeHttpRequests { authorize -> authorize.anyRequest().denyAll() }

    return http.build()
  }

  @Bean
  @Order(2)
  @Throws(Exception::class)
  fun authorizationServerSecurityFilterChain(
    http: HttpSecurity,
    issuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
  ): SecurityFilterChain {
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)

    http.getConfigurer(OAuth2AuthorizationServerConfigurer::class.java)
      .oidc { oidc ->
        oidc.providerConfigurationEndpoint(Customizer.withDefaults())
      }

    http
      .oauth2ResourceServer { resourceServer ->
        resourceServer.authenticationManagerResolver(issuerAuthenticationManagerResolver)
      }

    return http.build()
  }
}
