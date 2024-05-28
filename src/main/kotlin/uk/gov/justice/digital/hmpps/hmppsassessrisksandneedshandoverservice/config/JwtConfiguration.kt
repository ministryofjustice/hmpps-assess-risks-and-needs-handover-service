package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver

@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
data class JwtProperties(
  var issuers: List<JwtIssuerProperties> = emptyList(),
)

data class JwtIssuerProperties(
  var issuerName: String,
  var jwkSetUri: String,
  var issuerUri: String,
) {
  fun checkIssuerMatch(tokenIssuerUri: String): Boolean {
    return (tokenIssuerUri == issuerUri)
  }
}

@Configuration("jwt")
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfiguration(
  private val jwtProperties: JwtProperties,
) {
  fun issuerAuthenticationManagerResolver(): JwtIssuerAuthenticationManagerResolver {
    val issuerToJwkSetUri = jwtProperties.issuers.associateBy(
      { it.issuerUri },
      { it.jwkSetUri },
    )

    return JwtIssuerAuthenticationManagerResolver { issuer: String ->
      val jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuerToJwkSetUri[issuer]).build()
      val jwtAuthenticationProvider = JwtAuthenticationProvider(jwtDecoder)

      jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer))
      AuthenticationManager { authentication ->
        jwtAuthenticationProvider.authenticate(authentication)
      }
    }
  }

  fun isIssuedByHmppsAuth() = getIssuerByIssuerName("HMPPS Auth")?.let { isIssuer(it) }

  fun isIssuedByHmppsHandover() = getIssuerByIssuerName("HMPPS Handover")?.let { isIssuer(it) }

  fun isClientCredentialsGrant(): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication
    val grantType = AuthorizationGrantType.CLIENT_CREDENTIALS.value

    val jwt = authentication.principal as? Jwt
      ?: throw AccessDeniedException("Expected JWT authentication")

    val tokenGrantType = jwt.claims["grant_type"]?.toString()
      ?: throw AccessDeniedException("JWT token does not contain a grant type")

    if (tokenGrantType != grantType) {
      throw AccessDeniedException("Token needs to be of type $grantType")
    }

    return true
  }

  private fun isIssuer(issuer: JwtIssuerProperties): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication

    val jwt = authentication.principal as? Jwt
      ?: throw AccessDeniedException("Expected JWT authentication")

    val tokenIssuerUri = jwt.claims["iss"]?.toString()
      ?: throw AccessDeniedException("JWT token does not contain an issuer")

    if (!issuer.checkIssuerMatch(tokenIssuerUri)) {
      throw AccessDeniedException("Token needs to be issued by ${issuer.issuerName}")
    }

    return true
  }

  fun getIssuerByIssuerName(issuerName: String): JwtIssuerProperties? {
    return jwtProperties.issuers.find { it.issuerName == issuerName }
  }

  init {
    log.info("Application is configured to use the following JWT issuers:")
    jwtProperties.issuers.forEach { (issuerName, jwkSetUri) ->
      log.info("Issuer: $issuerName, JWK Set URI: $jwkSetUri")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
