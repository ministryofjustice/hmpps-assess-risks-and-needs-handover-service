package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException

class JwtConfigurationTest {
  private val hmppsAuthIssuer = JwtIssuerProperties(
    issuerName = "HMPPS Auth",
    jwkSetUri = "http://hmpps-auth:9090/auth/.well-known/jwks.json",
    issuerUri = "http://hmpps-auth:9090/auth/issuer",
  )

  private val handoverIssuer = JwtIssuerProperties(
    issuerName = "HMPPS Handover",
    jwkSetUri = "http://arns-handover:7070/oauth2/jwks",
    issuerUri = "http://arns-handover:7070",
  )

  private val jwtConfiguration = JwtConfiguration(
    JwtProperties(
      issuers = listOf(hmppsAuthIssuer, handoverIssuer),
    ),
  )

  @Test
  fun `should cache authentication manager per issuer`() {
    val first = jwtConfiguration.resolveIssuerAuthenticationManager(hmppsAuthIssuer.issuerUri)
    val second = jwtConfiguration.resolveIssuerAuthenticationManager(hmppsAuthIssuer.issuerUri)

    assertNotNull(first)
    assertSame(first, second)
  }

  @Test
  fun `should throw when issuer is unknown`() {
    assertThrows(AccessDeniedException::class.java) {
      jwtConfiguration.resolveIssuerAuthenticationManager("http://unknown-issuer")
    }
  }
}
