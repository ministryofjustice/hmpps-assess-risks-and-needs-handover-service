package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter
import org.springframework.security.web.authentication.AuthenticationConverter

class ScopeNormalizingAuthorizationRequestConverter(
  private val delegate: AuthenticationConverter = OAuth2AuthorizationCodeRequestAuthenticationConverter(),
  private val defaultScopes: Set<String> = setOf(OidcScopes.PROFILE),
  private val supportedScopes: Set<String> = defaultScopes,
) : AuthenticationConverter {

  override fun convert(request: HttpServletRequest): Authentication? {
    val authentication = delegate.convert(request)
    if (authentication !is OAuth2AuthorizationCodeRequestAuthenticationToken) {
      return authentication
    }

    val requestedScopes = authentication.scopes.orEmpty()
    val normalizedScopes = requestedScopes
      .filter { scope -> scope in supportedScopes }
      .toSet()
      .ifEmpty { defaultScopes }

    if (normalizedScopes == requestedScopes) {
      return authentication
    }

    return OAuth2AuthorizationCodeRequestAuthenticationToken(
      authentication.authorizationUri,
      authentication.clientId,
      authentication.principal as Authentication,
      authentication.redirectUri,
      authentication.state,
      normalizedScopes,
      authentication.additionalParameters,
    )
  }
}
