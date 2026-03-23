package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken
import org.springframework.security.web.authentication.AuthenticationConverter

class ScopeNormalizingAuthorizationRequestConverterTest {
  private val request = MockHttpServletRequest()
  private val principal = TestingAuthenticationToken("user", "password")

  @Test
  fun `should keep supported scopes when unsupported scopes are also requested`() {
    // Arrange
    val converter = ScopeNormalizingAuthorizationRequestConverter(
      delegate = authenticationConverter(tokenFor(scopes = setOf("openid", "profile"))),
    )

    // Act
    val authentication = converter.convert(request) as OAuth2AuthorizationCodeRequestAuthenticationToken

    // Assert
    assertEquals(setOf("profile"), authentication.scopes)
  }

  @Test
  fun `should default to profile when only unsupported scopes are requested`() {
    // Arrange
    val converter = ScopeNormalizingAuthorizationRequestConverter(
      delegate = authenticationConverter(tokenFor(scopes = setOf("openid", "email"))),
    )

    // Act
    val authentication = converter.convert(request) as OAuth2AuthorizationCodeRequestAuthenticationToken

    // Assert
    assertEquals(setOf("profile"), authentication.scopes)
  }

  @Test
  fun `should default to profile when scope is omitted`() {
    // Arrange
    val converter = ScopeNormalizingAuthorizationRequestConverter(
      delegate = authenticationConverter(tokenFor(scopes = null)),
    )

    // Act
    val authentication = converter.convert(request) as OAuth2AuthorizationCodeRequestAuthenticationToken

    // Assert
    assertEquals(setOf("profile"), authentication.scopes)
  }

  @Test
  fun `should return original authentication when delegate returns null`() {
    // Arrange
    val converter = ScopeNormalizingAuthorizationRequestConverter(
      delegate = authenticationConverter(authentication = null),
    )

    // Act
    val authentication = converter.convert(request)

    // Assert
    assertNull(authentication)
  }

  @Test
  fun `should return original authentication when delegate returns a different type`() {
    // Arrange
    val authentication = TestingAuthenticationToken("user", "password")
    val converter = ScopeNormalizingAuthorizationRequestConverter(
      delegate = authenticationConverter(authentication),
    )

    // Act
    val convertedAuthentication = converter.convert(request)

    // Assert
    assertSame(authentication, convertedAuthentication)
  }

  private fun tokenFor(scopes: Set<String>?): OAuth2AuthorizationCodeRequestAuthenticationToken = OAuth2AuthorizationCodeRequestAuthenticationToken(
    "http://localhost/oauth2/authorize",
    "test-client",
    principal,
    "http://localhost:3000/callback",
    "test-state",
    scopes,
    emptyMap(),
  )

  private fun authenticationConverter(authentication: Authentication?): AuthenticationConverter = AuthenticationConverter { authentication }
}
