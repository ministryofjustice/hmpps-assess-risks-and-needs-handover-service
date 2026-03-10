package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class WebClientConfigurationTest {
  private val webClientConfiguration = WebClientConfiguration(
    coordinatorApiUri = "http://localhost",
    timeout = Duration.ofSeconds(5),
    retryMaxRetries = 2,
    retryMinBackoff = Duration.ofMillis(1),
  )

  @Nested
  @DisplayName("retryingWebClientCustomizer")
  inner class RetryingWebClientCustomizer {
    @Test
    fun `should retry when the downstream API returns a retryable status before succeeding`() {
      val requestCount = AtomicInteger()
      val expectedSentencePlanId = UUID.randomUUID()
      val expectedAssessmentId = UUID.randomUUID()
      val response = exchangeFilterFunction().filter(
        clientRequest(),
        ExchangeFunction {
          Mono.defer {
            val currentAttempt = requestCount.incrementAndGet()

            if (currentAttempt < 3) {
              return@defer Mono.just(
                ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                  .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                  .body("""{"status":503}""")
                  .build(),
              )
            }

            Mono.just(
              ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(
                  """
                  {
                    "sentencePlanId": "$expectedSentencePlanId",
                    "sanAssessmentId": "$expectedAssessmentId",
                    "planVersion": 7
                  }
                  """.trimIndent(),
                )
                .build(),
            )
          }
        },
      )
      val result = response
        .flatMap { clientResponse -> clientResponse.bodyToMono(TestAssociationsResponse::class.java) }
        .block()

      assertEquals(3, requestCount.get())
      assertEquals(expectedSentencePlanId, result?.sentencePlanId)
      assertEquals(expectedAssessmentId, result?.sanAssessmentId)
      assertEquals(7, result?.planVersion)
    }

    @Test
    fun `should not retry when the downstream API returns a bad request`() {
      val requestCount = AtomicInteger()
      val exception = assertFailsWith<Exception> {
        exchangeFilterFunction().filter(
          clientRequest(),
          ExchangeFunction {
            Mono.defer {
              requestCount.incrementAndGet()
              Mono.just(
                ClientResponse.create(HttpStatus.BAD_REQUEST)
                  .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                  .body("""{"status":400}""")
                  .build(),
              )
            }
          },
        ).flatMap { clientResponse ->
          clientResponse.createException().flatMap { responseException -> Mono.error<ClientResponse>(responseException) }
        }.block()
      }

      assertEquals(1, requestCount.get())
      assertContains(exception.message.orEmpty(), "400 Bad Request")
    }

    private fun clientRequest(): ClientRequest = ClientRequest.create(HttpMethod.GET, URI.create("http://localhost/oasys/12345/associations?planVersion=7")).build()

    private fun exchangeFilterFunction() = webClientConfiguration.retryingExchangeFilterFunction()
  }
}

private data class TestAssociationsResponse(
  val sentencePlanId: UUID,
  val sanAssessmentId: UUID,
  val planVersion: Long,
)
