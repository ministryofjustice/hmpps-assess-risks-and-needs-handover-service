package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.web.util.UrlPathHelper
import java.util.UUID

class HandoverAuthenticationConverter(
  private val handoverEndpoint: String,
) : AuthenticationConverter {

  override fun convert(request: HttpServletRequest): Authentication {
    val path = UrlPathHelper.defaultInstance.getPathWithinApplication(request)
    val handoverCode = path.removePrefix("$handoverEndpoint/")

    if (handoverCode == path || handoverCode.contains('/')) {
      throw BadCredentialsException("Invalid handover code path")
    }

    val clientId = request.getParameter(CLIENT_ID_PARAM)
      ?.takeIf { it.isNotBlank() }
      ?: throw BadCredentialsException("Missing clientId")

    val parsedCode = runCatching { UUID.fromString(handoverCode) }
      .getOrElse { throw BadCredentialsException("Invalid handover code") }

    return HandoverAuthenticationRequestToken(
      handoverCode = parsedCode,
      clientId = clientId,
      redirectUri = request.getParameter(REDIRECT_URI_PARAM),
    )
  }

  private companion object {
    const val CLIENT_ID_PARAM = "clientId"
    const val REDIRECT_URI_PARAM = "redirectUri"
  }
}
