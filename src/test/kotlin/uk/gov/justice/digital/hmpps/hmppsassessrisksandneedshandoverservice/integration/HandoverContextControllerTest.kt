package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request.UpdateHandoverContextRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.WireMockExtension
import java.time.LocalDate
import java.util.*

@ExtendWith(WireMockExtension::class)
class HandoverContextControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var handoverContextRepository: HandoverContextRepository

  @Autowired
  lateinit var appConfiguration: AppConfiguration

  @Nested
  @DisplayName("updateContext")
  inner class UpdateContext {
    @Test
    fun `should return okay when updating the handover context with HMPPS Auth client credentials token `() {
      val handoverSessionId = UUID.randomUUID().toString()
      val oldHandoverContext = TestUtils.createHandoverContext(handoverSessionId)
      val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

      // Setup an initial handover context
      handoverContextRepository.save(oldHandoverContext)

      val response = webTestClient.post().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .bodyValue(newHandoverContext)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isOk
        .expectBody(HandoverContext::class.java)
        .returnResult()
        .responseBody

      assertThat(response.handoverSessionId).isEqualTo(newHandoverContext.handoverSessionId)
      assertThat(response.principal).isEqualTo(newHandoverContext.principal)
      assertThat(response.subject).isEqualTo(newHandoverContext.subject)
      assertThat(response.assessmentContext).isEqualTo(newHandoverContext.assessmentContext)
      assertThat(response.sentencePlanContext).isEqualTo(newHandoverContext.sentencePlanContext)
    }

    @Test
    fun `should return bad request when invalid request`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val newHandoverContext = UpdateHandoverContextRequest(
        principal = TestUtils.createPrincipal(),
        subject = SubjectDetails(
          crn = "this_is_far_too_loooooong_to_be_a_crn",
          pnc = "12345",
          nomisId = "",
          givenName = "Jenkins",
          familyName = "",
          dateOfBirth = LocalDate.of(1990, 1, 1),
          gender = 1,
          location = Location.PRISON,
          sexuallyMotivatedOffenceHistory = "YES",
        ),
        assessmentContext = TestUtils.createAssessmentContext(),
        sentencePlanContext = TestUtils.createSentencePlanContext(),
      )

      val response = webTestClient.post().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .bodyValue(newHandoverContext)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response.userMessage).contains("subject.familyName: size must be between 1 and 25")
      assertThat(response.userMessage).contains("subject.crn: size must be between 1 and 15")
    }

    @Test
    fun `should return forbidden when updating the handover context with a HMPPS ARNS Handover access token`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

      val response = webTestClient.post().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .bodyValue(newHandoverContext)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateHandoverToken(handoverSessionId)}")
        .exchange()
        .expectStatus().isForbidden
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response.developerMessage).isEqualTo("Token needs to be issued by HMPPS Auth")
    }

    @Test
    fun `should return unauthorized when updating the handover context without authorization`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

      webTestClient.post().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .bodyValue(newHandoverContext)
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return not found when updating the handover context but handover context does not exist`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

      webTestClient.post().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .bodyValue(newHandoverContext)
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GetContextByHandoverSessionId")
  inner class GetContextByHandoverSessionId {
    @Test
    fun `should return okay when getting the handover context with HMPPS Auth client credentials token`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

      // Setup an initial handover context
      handoverContextRepository.save(handoverContext)

      val response = webTestClient.get().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isOk
        .expectBody(HandoverContext::class.java)
        .returnResult()
        .responseBody

      assertThat(response).isEqualTo(handoverContext)
    }

    @Test
    fun `should return not found when getting the handover context but handover context does not exist`() {
      val handoverSessionId = UUID.randomUUID().toString()

      webTestClient.get().uri("${appConfiguration.self.endpoints.context}/$handoverSessionId")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("getContextByAuthentication")
  inner class GetContextByAuthentication {
    @Test
    fun `should return okay when getting the handover context using a HMPPS ARNS Handover access token`() {
      val handoverSessionId = UUID.randomUUID().toString()
      val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

      // Setup an initial handover context
      handoverContextRepository.save(handoverContext)

      val response = webTestClient.get().uri(appConfiguration.self.endpoints.context)
        .header("Authorization", "Bearer ${jwtHelper.generateHandoverToken(handoverSessionId)}")
        .exchange()
        .expectStatus().isOk
        .expectBody(HandoverContext::class.java)
        .returnResult()
        .responseBody

      assertThat(response).isEqualTo(handoverContext)
    }

    @Test
    fun `should return forbidden when updating the handover context using HMPPS Auth client credentials token `() {
      val handoverSessionId = UUID.randomUUID().toString()
      val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

      // Setup an initial handover context
      handoverContextRepository.save(handoverContext)

      val response = webTestClient.get().uri(appConfiguration.self.endpoints.context)
        .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
        .exchange()
        .expectStatus().isForbidden
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody

      assertThat(response.developerMessage).isEqualTo("Token needs to be issued by HMPPS Handover")
    }
  }
}
