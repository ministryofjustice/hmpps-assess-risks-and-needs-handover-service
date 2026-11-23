package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class CoordinatorServiceTest {
  private val oasysAssessmentPk = "12345"
  private val sanAssessmentId = UUID.randomUUID()
  private val sentencePlanId = UUID.randomUUID()
  private val tieringAssessmentId = UUID.randomUUID()
  private val requests = mutableListOf<ClientRequest>()

  private fun coordinatorService(
    status: HttpStatus = HttpStatus.OK,
    body: String = associationsResponseBody(),
  ) = CoordinatorService(
    WebClient.builder()
      .baseUrl("http://localhost")
      .exchangeFunction { request ->
        requests.add(request)
        Mono.just(
          ClientResponse.create(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .build(),
        )
      }
      .build(),
  )

  private fun associationsResponseBody() = """
    {
      "sanAssessmentId": "$sanAssessmentId",
      "sanAssessmentVersion": 3,
      "sentencePlanId": "$sentencePlanId",
      "sentencePlanVersion": 7,
      "tieringAssessmentId": "$tieringAssessmentId",
      "tieringAssessmentVersion": 8
    }
  """.trimIndent()

  @Nested
  @DisplayName("getAssociations")
  inner class GetAssociations {
    @Test
    fun `should include both plan and assessment versions as query params when provided`() {
      coordinatorService().getAssociations(oasysAssessmentPk, 7, 3)

      val url = requests.single().url()
      assertEquals("/oasys/$oasysAssessmentPk/associations", url.path)
      assertEquals(setOf("planVersion=7", "assessmentVersion=3"), url.query.split("&").toSet())
    }

    @Test
    fun `should only include the assessment version query param when no plan version is provided`() {
      coordinatorService().getAssociations(oasysAssessmentPk, null, 3)

      assertEquals("assessmentVersion=3", requests.single().url().query)
    }

    @Test
    fun `should only include the plan version query param when no assessment version is provided`() {
      coordinatorService().getAssociations(oasysAssessmentPk, 7, null)

      assertEquals("planVersion=7", requests.single().url().query)
    }

    @Test
    fun `should not include any query params when no versions are provided`() {
      coordinatorService().getAssociations(oasysAssessmentPk, null, null)

      assertNull(requests.single().url().query)
    }

    @Test
    fun `should map the associations response including the SAN assessment version`() {
      val result = coordinatorService().getAssociations(oasysAssessmentPk, 7, 3)

      assertEquals(sanAssessmentId, result.sanAssessmentId)
      assertEquals(3, result.sanAssessmentVersion)
      assertEquals(sentencePlanId, result.sentencePlanId)
      assertEquals(7, result.sentencePlanVersion)
      assertEquals(tieringAssessmentId, result.tieringAssessmentId)
      assertEquals(8, result.tieringAssessmentVersion)
    }

    @Test
    fun `should map a missing SAN assessment version to null`() {
      val result = coordinatorService(body = """{ "sanAssessmentId": "$sanAssessmentId" }""")
        .getAssociations(oasysAssessmentPk, null, null)

      assertEquals(sanAssessmentId, result.sanAssessmentId)
      assertNull(result.sanAssessmentVersion)
    }

    @Test
    fun `should throw when the coordinator returns an error response`() {
      val exception = assertFailsWith<Exception> {
        coordinatorService(status = HttpStatus.NOT_FOUND, body = "{}")
          .getAssociations(oasysAssessmentPk, null, 3)
      }

      assertContains(exception.message.orEmpty(), "404")
    }
  }
}
