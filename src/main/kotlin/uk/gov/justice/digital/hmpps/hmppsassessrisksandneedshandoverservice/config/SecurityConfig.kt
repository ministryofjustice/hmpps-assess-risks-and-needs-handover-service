package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.AuthenticationFilter
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.UnauthorizedHandler
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security.HandoverAuthenticationConverter
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security.HandoverAuthenticationFailureHandler
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security.HandoverAuthenticationProvider
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security.HandoverAuthenticationSuccessHandler

@Configuration
class SecurityConfig {

  @Bean
  @Order(3)
  fun securityChain(
    http: HttpSecurity,
    appConfiguration: AppConfiguration,
    issuerAuthenticationManagerResolver: JwtIssuerAuthenticationManagerResolver,
    handoverAuthenticationProvider: HandoverAuthenticationProvider,
    handoverAuthenticationFailureHandler: HandoverAuthenticationFailureHandler,
    handoverAuthenticationSuccessHandler: HandoverAuthenticationSuccessHandler,
  ): SecurityFilterChain {
    val handoverAuthenticationFilter = AuthenticationFilter(
      ProviderManager(handoverAuthenticationProvider),
      HandoverAuthenticationConverter(appConfiguration.self.endpoints.handover),
    ).apply {
      setRequestMatcher(
        PathPatternRequestMatcher.pathPattern(
          HttpMethod.GET,
          "${appConfiguration.self.endpoints.handover}/{handoverCode}",
        ),
      )
      setSecurityContextRepository(HttpSessionSecurityContextRepository())
      setSuccessHandler(handoverAuthenticationSuccessHandler)
      setFailureHandler(handoverAuthenticationFailureHandler)
    }

    http.authorizeHttpRequests { request ->
      // App
      request
        .requestMatchers(
          HttpMethod.GET,
          "${appConfiguration.self.endpoints.handover}/*",
        ).permitAll()

      // Status, docs and reporting
      request
        .requestMatchers(
          HttpMethod.GET,
          // Health and Info
          "/health/**",
          "/info",
          // Swagger
          "/v3/api-docs/**",
          "/swagger-ui.html",
          "/swagger-ui/**",
          // Error pages
          "/access-denied",
          // Static resources
          "/css/**",
          "/webjars/**",
        ).permitAll()

      // Catch all
      request
        .anyRequest().authenticated()
    }

    return http
      .csrf { csrf -> csrf.disable() }
      .exceptionHandling { it.authenticationEntryPoint(UnauthorizedHandler()) }
      .logout { logout ->
        logout
          .logoutSuccessUrl("https://www.gov.uk")
          .logoutUrl("/sign-out")
      }
      .oauth2ResourceServer { resourceServer ->
        resourceServer
          .authenticationManagerResolver(issuerAuthenticationManagerResolver)
      }
      .addFilterBefore(handoverAuthenticationFilter, AnonymousAuthenticationFilter::class.java)
      .build()
  }

  @Bean
  fun sessionRegistry(): SessionRegistry = SessionRegistryImpl()
}
