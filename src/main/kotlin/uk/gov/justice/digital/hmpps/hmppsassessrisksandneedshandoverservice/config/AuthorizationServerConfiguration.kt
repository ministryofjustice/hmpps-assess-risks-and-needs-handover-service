package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.validator.WildcardRedirectUriValidator
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.UnauthorizedHandler
import java.util.ArrayList
import java.util.function.Consumer

@Configuration
class AuthorizationServerConfiguration {
  @Bean
  @Order(1)
  @Throws(Exception::class)
  fun denyUnusedOauth2EndpointsSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    val urls = arrayOf(
      // Token management endpoints
      "/oauth2/introspect",
      "/oauth2/revoke",
      // Device Authorization Flow
      "/oauth2/device_authorization",
      // OAuth2 Consent Flow
      "/oauth2/consent",
      "/oauth2/decision",
      // OpenID Connect
      "/connect/logout",
      "/connect/register",
      "/userinfo",
      "/.well-known/openid-configuration",
    )

    http
      .securityMatcher { e -> urls.any { url -> url == e.requestURI } }
      .authorizeHttpRequests { authorize -> authorize.anyRequest().denyAll() }
      .exceptionHandling { it.authenticationEntryPoint(UnauthorizedHandler()) }

    return http.build()
  }

  @Bean
  @Order(2)
  @Throws(Exception::class)
  fun authorizationServerSecurityFilterChain(
    http: HttpSecurity,
    issuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
    registeredClientRepository: RegisteredClientRepository,
    authorizationService: OAuth2AuthorizationService,
  ): SecurityFilterChain {
    http.exceptionHandling { it.authenticationEntryPoint(UnauthorizedHandler()) }

    val configurer = OAuth2AuthorizationServerConfigurer()
    val clientSecretAuthenticationProvider = clientSecretAuthenticationProvider(
      registeredClientRepository,
      authorizationService,
    )

    http
      .securityMatcher(configurer.endpointsMatcher)
      .with(configurer, Customizer.withDefaults())
      .authorizeHttpRequests { authorize -> authorize.anyRequest().authenticated() }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.NEVER) }

    configurer.authorizationEndpoint { authorizationEndpoint ->
      authorizationEndpoint.authorizationRequestConverter(
        ScopeNormalizingAuthorizationRequestConverter(),
      )
      authorizationEndpoint.authenticationProviders(
        configureAuthenticationValidator(),
      )
    }
    configurer.clientAuthentication { clientAuthentication ->
      clientAuthentication.authenticationProviders(
        configureClientAuthenticationProviders(clientSecretAuthenticationProvider),
      )
    }

    http
      .oauth2ResourceServer { resourceServer ->
        resourceServer.authenticationManagerResolver(issuerAuthenticationManagerResolver)
      }

    return http.build()
  }
}

private fun clientSecretAuthenticationProvider(
  registeredClientRepository: RegisteredClientRepository,
  authorizationService: OAuth2AuthorizationService,
): ClientSecretAuthenticationProvider {
  val authenticationProvider = ClientSecretAuthenticationProvider(
    registeredClientRepository,
    authorizationService,
  )
  authenticationProvider.setPasswordEncoder(NoopAwareClientSecretPasswordEncoder())

  return authenticationProvider
}

private fun configureAuthenticationValidator(): Consumer<List<AuthenticationProvider>> = Consumer<List<AuthenticationProvider>> { authenticationProviders ->
  authenticationProviders.forEach { authenticationProvider ->
    if (authenticationProvider is OAuth2AuthorizationCodeRequestAuthenticationProvider) {
      val authenticationValidator: Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> =
        WildcardRedirectUriValidator()
          .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR)

      authenticationProvider
        .setAuthenticationValidator(authenticationValidator)
    }
  }
}

private fun configureClientAuthenticationProviders(
  clientSecretAuthenticationProvider: ClientSecretAuthenticationProvider,
): Consumer<List<AuthenticationProvider>> = Consumer<List<AuthenticationProvider>> { authenticationProviders ->
  val mutableAuthenticationProviders = authenticationProviders as ArrayList<AuthenticationProvider>

  val index = mutableAuthenticationProviders.indexOfFirst { authenticationProvider ->
    authenticationProvider is ClientSecretAuthenticationProvider
  }

  if (index >= 0) {
    mutableAuthenticationProviders[index] = clientSecretAuthenticationProvider
  } else {
    mutableAuthenticationProviders.add(clientSecretAuthenticationProvider)
  }
}
