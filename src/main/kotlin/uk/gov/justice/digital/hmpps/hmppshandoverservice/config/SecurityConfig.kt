package uk.gov.justice.digital.hmpps.hmppshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.core.session.SessionRegistryImpl
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
  private val jwtConfiguration: JwtConfiguration,
) {

  @Bean
  @Order(3)
  fun securityChain(
    http: HttpSecurity,
  ): SecurityFilterChain {
    val signOutUrl = "/sign-out"

    return http
      .authorizeHttpRequests { requests ->
        requests
          .requestMatchers(
            "/health/**",
            "/info",
            "/ping",
            "/swagger-ui.html",
            "/swagger-ui/**",
          ).permitAll()
          .requestMatchers(HttpMethod.GET, "/handover/*").permitAll()
          .anyRequest().authenticated()
      }
      .logout { o ->
        run {
          o.logoutUrl(signOutUrl)
          o.logoutSuccessUrl("https://www.gov.uk")
        }
      }
      .oauth2ResourceServer { resourceServer -> resourceServer.authenticationManagerResolver(jwtConfiguration.issuerAuthenticationManagerResolver()) }
      .build()
  }

  @Bean
  fun sessionRegistry(): SessionRegistry {
    return SessionRegistryImpl()
  }
}
