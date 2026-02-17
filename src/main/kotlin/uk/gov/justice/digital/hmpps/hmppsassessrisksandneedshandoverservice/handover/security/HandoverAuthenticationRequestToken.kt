package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import java.util.UUID

class HandoverAuthenticationRequestToken(
  val handoverCode: UUID,
  val clientId: String,
  val redirectUri: String?,
) : AbstractAuthenticationToken(emptyList()) {
  init {
    super.setAuthenticated(false)
  }

  override fun getCredentials(): Any = handoverCode

  override fun getPrincipal(): Any = clientId

  override fun setAuthenticated(isAuthenticated: Boolean) {
    require(!isAuthenticated) { "Cannot mark HandoverAuthenticationRequestToken as authenticated" }
    super.setAuthenticated(false)
  }
}
