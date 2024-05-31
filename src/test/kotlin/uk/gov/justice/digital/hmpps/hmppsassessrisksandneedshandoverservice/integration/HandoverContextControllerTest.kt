package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.repository.HandoverContextRepository
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.WireMockExtension
import java.util.*

@ExtendWith(WireMockExtension::class)
class HandoverContextControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var handoverContextRepository: HandoverContextRepository

  @Test
  fun `update the handover context using handoverSessionID with auth credentials token should return okay`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val oldHandoverContext = TestUtils.createHandoverContext(handoverSessionId)
    val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

    // Setup an initial handover context
    handoverContextRepository.save(oldHandoverContext)

    val response = webTestClient.post().uri("/context/$handoverSessionId")
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
  fun `update the handover context using handover access token should return forbidden`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val oldHandoverContext = TestUtils.createHandoverContext(handoverSessionId)
    val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

    // Setup an initial handover context
    handoverContextRepository.save(oldHandoverContext)

    val response = webTestClient.post().uri("/context/$handoverSessionId")
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
  fun `update the handover context but handover context does not exist should return not found`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val newHandoverContext = TestUtils.createHandoverContext(handoverSessionId)

    webTestClient.post().uri("/context/$handoverSessionId")
      .bodyValue(newHandoverContext)
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `getting the handover context using handoverSessionID with auth credentials token should return okay`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

    // Setup an initial handover context
    handoverContextRepository.save(handoverContext)

    val response = webTestClient.get().uri("/context/$handoverSessionId")
      .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
      .exchange()
      .expectStatus().isOk
      .expectBody(HandoverContext::class.java)
      .returnResult()
      .responseBody

    assertThat(response).isEqualTo(handoverContext)
  }

  @Test
  fun `getting the handover context but handover context does not exist should return not found`() {
    val handoverSessionId = UUID.randomUUID().toString()

    webTestClient.get().uri("/context/$handoverSessionId")
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `getting the handover context using just a handover access token`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

    // Setup an initial handover context
    handoverContextRepository.save(handoverContext)

    val response = webTestClient.get().uri("/context")
      .header("Authorization", "Bearer ${jwtHelper.generateHandoverToken(handoverSessionId)}")
      .exchange()
      .expectStatus().isOk
      .expectBody(HandoverContext::class.java)
      .returnResult()
      .responseBody

    assertThat(response).isEqualTo(handoverContext)
  }

  @Test
  fun `update the handover context using just auth credentials token should return forbidden`() {
    val handoverSessionId = UUID.randomUUID().toString()
    val handoverContext = TestUtils.createHandoverContext(handoverSessionId)

    // Setup an initial handover context
    handoverContextRepository.save(handoverContext)

    val response = webTestClient.get().uri("/context")
      .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
      .exchange()
      .expectStatus().isForbidden
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response.developerMessage).isEqualTo("Token needs to be issued by HMPPS Handover")
  }
}
