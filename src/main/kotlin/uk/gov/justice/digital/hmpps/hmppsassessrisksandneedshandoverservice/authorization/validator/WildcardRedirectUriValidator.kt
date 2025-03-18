package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import java.util.function.Consumer
import java.util.regex.Pattern

class WildcardRedirectUriValidator : Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {
  override fun accept(authenticationContext: OAuth2AuthorizationCodeRequestAuthenticationContext) {
    val authRequest: OAuth2AuthorizationCodeRequestAuthenticationToken = authenticationContext.getAuthentication()
    val registeredClient: RegisteredClient = authenticationContext.registeredClient
    val requestedRedirectUri: String? = authRequest.redirectUri

    if (requestedRedirectUri.isNullOrBlank()) {
      throwError()
    }

    val matched = registeredClient.redirectUris.any { registeredUri ->
      matchesWildcardSubdomain(registeredUri, requestedRedirectUri!!)
    }

    if (!matched) {
      throwError()
    }
  }

  private fun matchesWildcardSubdomain(registeredUri: String, requestedUri: String): Boolean {
    if (!registeredUri.contains("*")) {
      return registeredUri == requestedUri
    }

    val regex = registeredUri
      .replace(".", "\\.")
      .replace("*\\.", "[^.]+\\.")

    val pattern = Pattern.compile("^$regex$")
    return pattern.matcher(requestedUri).matches()
  }

  private fun throwError() {
    val error = OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST)
    throw OAuth2AuthorizationCodeRequestAuthenticationException(error, null)
  }
}
