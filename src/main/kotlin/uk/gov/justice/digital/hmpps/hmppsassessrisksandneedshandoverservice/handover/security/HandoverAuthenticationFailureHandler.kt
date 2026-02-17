package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class HandoverAuthenticationFailureHandler : AuthenticationFailureHandler {
  override fun onAuthenticationFailure(
    request: HttpServletRequest,
    response: HttpServletResponse,
    exception: AuthenticationException,
  ) {
    response.status = HttpStatus.FOUND.value()
    response.setHeader("Location", ACCESS_DENIED_PATH)
  }

  private companion object {
    private const val ACCESS_DENIED_PATH = "/access-denied"
  }
}
