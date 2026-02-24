package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.security

import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class HandoverAuthenticationSuccessHandlerTest {
  private val handler = HandoverAuthenticationSuccessHandler()

  @Test
  fun `should redirect to configured uri and stop filter chain`() {
    val request = MockHttpServletRequest()
    request.setAttribute(HandoverAuthenticationSuccessHandler.REDIRECT_URI_REQUEST_ATTRIBUTE, "https://example.com/callback")
    val response = MockHttpServletResponse()
    val chain = mockk<FilterChain>(relaxed = true)
    val authentication = mockk<Authentication>()

    handler.onAuthenticationSuccess(request, response, chain, authentication)

    assertThat(response.redirectedUrl).isEqualTo("https://example.com/callback")
    verify(exactly = 0) { chain.doFilter(any(), any()) }
  }
}
