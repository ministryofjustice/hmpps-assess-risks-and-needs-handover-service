package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.TestUtils
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.*
import java.time.LocalDate

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Handover context Tests")
class HandoverContextControllerTest : IntegrationTestBase() {
  private final val handoverSessionId = "testSessionId"
  private val requestBody = TestUtils.createHandoverContext(handoverSessionId)

  @Test
  fun `Update the handover context should return unauthorized when no auth token`() {
    webTestClient.post().uri("/context/abc")
      .header("Content-Type", "application/json")
      .bodyValue(requestBody)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `Get handover context should return unauthorized when no auth token`() {
    webTestClient.get().uri("abc")
      .header("Content-Type", "application/json")
      .exchange()
      .expectStatus().isUnauthorized
  }

//  @Test
//  fun `Update the handover context should return forbidden when no role`() {
//    webTestClient.post().uri("/context/abc")
//      .header("Content-Type", "application/json")
//      .headers(setAuthorisation(roles = listOf("abc")))
//      .bodyValue(requestBody)
//      .exchange()
//      .expectStatus().isForbidden
//  }
}