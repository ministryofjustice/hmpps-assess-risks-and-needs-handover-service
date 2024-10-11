package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class UnauthorizedHandler : AuthenticationEntryPoint {
  override fun commence(
    request: HttpServletRequest?,
    response: HttpServletResponse?,
    authException: AuthenticationException?,
  ) {
    authException?.let {
      log.info("Auth exception: {}", authException.message)
      response?.sendRedirect("${request?.contextPath}/access-denied")
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
