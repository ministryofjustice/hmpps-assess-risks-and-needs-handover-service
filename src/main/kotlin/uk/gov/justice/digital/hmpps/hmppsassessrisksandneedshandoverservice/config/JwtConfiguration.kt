package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
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
  val issuers: List<JwtIssuerProperties> = emptyList(),
)

data class JwtIssuerProperties(
  val issuerName: String,
  val jwkSetUri: String,
  val issuerUri: String,
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
  @Bean
  fun issuerAuthenticationManagerResolver(): JwtIssuerAuthenticationManagerResolver {
    return JwtIssuerAuthenticationManagerResolver { issuerUri: String ->
      val issuer = getIssuerByIssuerUri(issuerUri)
        ?: throw AccessDeniedException("Invalid issuer: $issuerUri")

      val jwtDecoder = NimbusJwtDecoder.withJwkSetUri(issuer.jwkSetUri)
        .build()
        .apply {
          setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri))
        }

      AuthenticationManager { authentication ->
        JwtAuthenticationProvider(jwtDecoder).authenticate(authentication)
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

  private fun getIssuerByIssuerUri(issuerUri: String): JwtIssuerProperties? {
    return jwtProperties.issuers.find { it.issuerUri == issuerUri }
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

  private fun getIssuerByIssuerName(issuerName: String): JwtIssuerProperties? {
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
