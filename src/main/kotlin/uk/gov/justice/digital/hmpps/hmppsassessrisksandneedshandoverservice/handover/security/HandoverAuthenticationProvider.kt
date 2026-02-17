package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InternalAuthenticationServiceException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.extensions.isSubdomainOf
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.UseHandoverLinkResult
import java.net.URI

@Component
class HandoverAuthenticationProvider(
  private val handoverService: HandoverService,
  private val appConfiguration: AppConfiguration,
) : AuthenticationProvider {

  override fun authenticate(authentication: Authentication): Authentication {
    val requestToken = authentication as? HandoverAuthenticationRequestToken
      ?: throw BadCredentialsException("Unsupported authentication token")

    val client = appConfiguration.clients[requestToken.clientId]
      ?: throw BadCredentialsException("Client not found")

    val redirectUri = resolveRedirectUri(client, requestToken.redirectUri)

    return when (val result = handoverService.consumeAndExchangeHandover(requestToken.handoverCode)) {
      is UseHandoverLinkResult.Success -> {
        getCurrentRequest().setAttribute(HandoverAuthenticationSuccessHandler.REDIRECT_URI_REQUEST_ATTRIBUTE, redirectUri)
        result.authenticationToken
      }
      UseHandoverLinkResult.HandoverLinkNotFound -> throw BadCredentialsException("Handover link not found")
      UseHandoverLinkResult.HandoverLinkAlreadyUsed -> throw BadCredentialsException("Handover link already used")
    }
  }

  override fun supports(authentication: Class<*>): Boolean = HandoverAuthenticationRequestToken::class.java.isAssignableFrom(authentication)

  private fun resolveRedirectUri(
    client: AppConfiguration.Client,
    requestedRedirectUri: String?,
  ): String {
    if (requestedRedirectUri.isNullOrBlank()) {
      return client.handoverRedirectUri
    }

    val configuredUri = parseUri(client.handoverRedirectUri)
    val requestedUri = parseUri(requestedRedirectUri)

    if (requestedUri != configuredUri && !requestedUri.isSubdomainOf(configuredUri)) {
      throw BadCredentialsException("Invalid redirectUri")
    }

    return requestedUri.toString()
  }

  private fun parseUri(uri: String): URI = runCatching { URI(uri) }
    .getOrElse { throw BadCredentialsException("Invalid redirectUri") }

  private fun getCurrentRequest(): HttpServletRequest {
    val requestAttributes = RequestContextHolder.getRequestAttributes()
    if (requestAttributes !is ServletRequestAttributes) {
      throw InternalAuthenticationServiceException("No servlet request available for handover authentication")
    }
    return requestAttributes.request
  }
}
