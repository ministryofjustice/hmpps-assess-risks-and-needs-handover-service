package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.JwkProperties
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.JwtIssuerProperties
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.JwtProperties
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper(
  private val jwtProperties: JwtProperties,
  private val jwkProperties: JwkProperties,
) {
  private fun getIssuerByIssuerName(issuerName: String): JwtIssuerProperties? {
    return jwtProperties.issuers.find { it.issuerName == issuerName }
  }

  fun generateHandoverToken(handoverSessionId: UUID, grantType: AuthorizationGrantType = AuthorizationGrantType.JWT_BEARER): String {
    val hmppsHandover = getIssuerByIssuerName("HMPPS Handover")
      ?: throw IllegalStateException()

    val claimsSet = JWTClaimsSet.Builder()
      .issuer(hmppsHandover.issuerUri)
      .issueTime(Date())
      .subject(handoverSessionId.toString())
      .claim("grant_type", grantType.value)
      .expirationTime(Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour expiry
      .jwtID(WireMockExtension.KEY_ID)
      .build()

    val signedJWT = SignedJWT(
      JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwkProperties.keyId).build(),
      claimsSet,
    )

    signedJWT.sign(RSASSASigner(jwkProperties.decode().private))

    return signedJWT.serialize()
  }

  fun generateAuthToken(grantType: AuthorizationGrantType = AuthorizationGrantType.CLIENT_CREDENTIALS): String {
    val hmppsAuth = getIssuerByIssuerName("HMPPS Auth")
      ?: throw IllegalStateException()

    val claimsSet = JWTClaimsSet.Builder()
      .issuer(hmppsAuth.issuerUri)
      .issueTime(Date())
      .claim("grant_type", grantType.value)
      .expirationTime(Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour expiry
      .jwtID(WireMockExtension.KEY_ID)
      .build()

    val signedJWT = SignedJWT(
      JWSHeader.Builder(JWSAlgorithm.RS256).keyID(WireMockExtension.KEY_ID).build(),
      claimsSet,
    )

    signedJWT.sign(RSASSASigner(WireMockExtension.keyPair.private))

    return signedJWT.serialize()
  }
}
