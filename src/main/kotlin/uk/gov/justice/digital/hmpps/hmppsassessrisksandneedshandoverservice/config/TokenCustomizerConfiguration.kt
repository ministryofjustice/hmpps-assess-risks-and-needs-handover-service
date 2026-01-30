package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverAuthDetails

@Configuration
class TokenCustomizerConfiguration {

  @Bean
  fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> = OAuth2TokenCustomizer { context ->
    if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
      val authentication = context.getPrincipal<Authentication>()
      val details = authentication.details as? HandoverAuthDetails

      if (details != null) {
        context.claims.claims { claims ->
          claims["handover_session_id"] = details.handoverSessionId.toString()
          claims["user_name"] = details.principal.identifier
          claims["name"] = details.principal.displayName
          claims["auth_source"] = "OASys"
          claims["user_id"] = details.principal.identifier
        }
        context.claims.subject(details.principal.identifier)
      }
    }
  }
}
