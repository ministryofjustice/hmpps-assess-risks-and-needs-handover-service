package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.WireMockExtension
import java.util.*

@ExtendWith(WireMockExtension::class)
class HandoverControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var handoverTokenRepository: HandoverTokenRepository

  @Autowired
  lateinit var appConfiguration: AppConfiguration

  @Autowired
  lateinit var handoverService: HandoverService

  @Value("\${server.servlet.session.cookie.name}")
  lateinit var sessionCookieName: String

  @Nested
  @DisplayName("createHandoverLink")
  inner class CreateHandoverLink {
    @Test
    fun `should return okay when authenticated`() {
      val handoverRequest = TestUtils.createHandoverRequest()

      val response = webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isOk
        .expectBody(CreateHandoverLinkResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response.handoverLink).startsWith(appConfiguration.self.externalUrl)
      assertThat(response.handoverSessionId).isNotEmpty
    }

    @Test
    fun `should return forbidden when authentication is invalid`() {
      val handoverRequest = TestUtils.createHandoverRequest()

      val response = webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateHandoverToken("123")}")
        .exchange()
        .expectStatus().isForbidden
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response!!.developerMessage).isEqualTo("Token needs to be issued by HMPPS Auth")
    }

    @Test
    fun `should return forbidden when unauthenticated`() {
      val handoverRequest = TestUtils.createHandoverRequest()

      webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("useHandoverLink")
  inner class UseHandoverLink {
    @Test
    fun `should return found when using handover link with valid code `() {
      val clientId = "test-client"
      val client: AppConfiguration.Client = appConfiguration.clients[clientId]
        ?: throw IllegalStateException()

      val handoverToken = handoverTokenRepository.save(
        HandoverToken(
          handoverSessionId = UUID.randomUUID().toString(),
          principal = TestUtils.createPrincipal(),
        ),
      )

      webTestClient.get().uri("/handover/${handoverToken.code}?clientId=$clientId")
        .exchange()
        .expectStatus().isFound
        .expectHeader().valueEquals("Location", client.handoverRedirectUri)
        .expectCookie().exists(sessionCookieName)
    }

    @Test
    fun `should return not found when using handover link with invalid code `() {
      val clientId = "test-client"
      val handoverCode = UUID.randomUUID().toString()

      val response = webTestClient.get().uri("/handover/$handoverCode?clientId=$clientId")
        .exchange()
        .expectStatus().isNotFound
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).isEqualTo("Handover link expired or not found")
    }

    @Test
    fun `should return conflict when using handover link with already used code`() {
      val clientId = "test-client"

      val handoverToken = handoverTokenRepository.save(
        HandoverToken(
          handoverSessionId = UUID.randomUUID().toString(),
          principal = TestUtils.createPrincipal(),
        ),
      )

      handoverService.consumeAndExchangeHandover(handoverToken.code)

      val response = webTestClient.get().uri("/handover/${handoverToken.code}?clientId=$clientId")
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody(String::class.java)
        .returnResult()
        .responseBody

      assertThat(response).isEqualTo("Handover link has already been used")
    }
  }
}
