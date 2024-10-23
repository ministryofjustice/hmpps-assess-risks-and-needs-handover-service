package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.repository.HandoverTokenRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response.CreateHandoverLinkResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.service.HandoverService
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.WireMockExtension
import java.time.LocalDate
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
      assertThat(response.handoverSessionId).toString().isNotEmpty()
    }

    @Test
    fun `should return bad request when invalid request`() {
      val handoverRequest = CreateHandoverLinkRequest(
        user = TestUtils.createPrincipal(),
        subjectDetails = SubjectDetails(
          crn = "12345",
          pnc = "12345",
          nomisId = "",
          givenName = "",
          familyName = "Jenkins",
          dateOfBirth = LocalDate.of(1990, 1, 1),
          gender = 1,
          location = Location.PRISON,
          sexuallyMotivatedOffenceHistory = "invalid_answer",
        ),
        oasysAssessmentPk = "123",
        assessmentVersion = 1,
        planVersion = 1,
      )

      val response = webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response.userMessage).contains("subjectDetails.sexuallyMotivatedOffenceHistory: must be either 'YES' or 'NO'")
      assertThat(response.userMessage).contains("subjectDetails.givenName: size must be between 1 and 25")
    }

    @Test
    fun `should return forbidden when auth token is invalid`() {
      val handoverRequest = TestUtils.createHandoverRequest()

      val response = webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateHandoverToken(UUID.randomUUID())}")
        .exchange()
        .expectStatus().isForbidden
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response!!.developerMessage).isEqualTo("Token needs to be issued by HMPPS Auth")
    }

    @Test
    fun `should return access denied when unauthenticated`() {
      val handoverRequest = TestUtils.createHandoverRequest()

      webTestClient.post().uri(appConfiguration.self.endpoints.handover)
        .bodyValue(handoverRequest)
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isFound
        .expectHeader().valueMatches("Location", "^https?://.*:[0-9]+/access-denied$")
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
          handoverSessionId = UUID.randomUUID(),
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
    fun `should return access denied when using handover link with invalid code `() {
      val clientId = "test-client"
      val handoverCode = UUID.randomUUID().toString()

      webTestClient.get().uri("/handover/$handoverCode?clientId=$clientId")
        .exchange()
        .expectStatus().isFound
        .expectHeader().valueEquals("Location", "/access-denied")
        .expectCookie().doesNotExist(sessionCookieName)
    }

    @Test
    fun `should return access denied when using handover link with already used code`() {
      val clientId = "test-client"

      val handoverToken = handoverTokenRepository.save(
        HandoverToken(
          handoverSessionId = UUID.randomUUID(),
          principal = TestUtils.createPrincipal(),
        ),
      )

      handoverService.consumeAndExchangeHandover(handoverToken.code)

      webTestClient.get().uri("/handover/${handoverToken.code}?clientId=$clientId")
        .exchange()
        .expectStatus().isFound
        .expectHeader().valueEquals("Location", "/access-denied")
        .expectCookie().doesNotExist(sessionCookieName)
    }
  }
}
