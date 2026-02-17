package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class HandoverAuthenticationSuccessHandler : AuthenticationSuccessHandler {
  override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
  ) {
    val redirectUri = request.getAttribute(REDIRECT_URI_REQUEST_ATTRIBUTE)?.toString() ?: ACCESS_DENIED_PATH
    response.sendRedirect(redirectUri)
  }

  companion object {
    const val REDIRECT_URI_REQUEST_ATTRIBUTE = "handover.redirect-uri"
    private const val ACCESS_DENIED_PATH = "/access-denied"
  }
}
