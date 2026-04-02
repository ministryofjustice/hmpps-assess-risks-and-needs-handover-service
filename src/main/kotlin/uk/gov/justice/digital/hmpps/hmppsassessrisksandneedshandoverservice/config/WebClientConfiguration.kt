package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeoutException

@Configuration
class WebClientConfiguration(
  @param:Value("\${app.services.coordinator-api.base-url}") private val coordinatorApiUri: String,
  @param:Value("\${api.timeout:20s}") private val timeout: Duration,
  @param:Value("\${api.retry.max-retries:2}") private val retryMaxRetries: Long,
  @param:Value("\${api.retry.min-backoff:250ms}") private val retryMinBackoff: Duration,
) {
  private val retryableStatusCodes = setOf(429, 502, 503, 504)

  @Bean
  fun retryingWebClientCustomizer(): WebClientCustomizer = WebClientCustomizer { builder ->
    builder.filter(retryingExchangeFilterFunction())
  }

  @Bean
  fun coordinatorApiWebClient(authorizedClientManager: org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "coordinator-api",
    url = coordinatorApiUri,
    timeout,
  )

  internal fun retryingExchangeFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
    next.exchange(request)
      .flatMap(::toRetryableResponse)
      .retryWhen(createRetrySpec())
  }

  private fun toRetryableResponse(response: ClientResponse): Mono<ClientResponse> {
    if (!retryableStatusCodes.contains(response.statusCode().value())) {
      return Mono.just(response)
    }

    return response.createException().flatMap { exception -> Mono.error<ClientResponse>(exception) }
  }

  private fun createRetrySpec(): Retry = Retry
    .backoff(retryMaxRetries, retryMinBackoff)
    .jitter(0.5)
    .filter(::isRetryableException)
    .onRetryExhaustedThrow { _, signal -> signal.failure() }

  private fun isRetryableException(exception: Throwable): Boolean {
    if (exception is org.springframework.web.reactive.function.client.WebClientResponseException) {
      return retryableStatusCodes.contains(exception.statusCode.value())
    }

    if (exception is org.springframework.web.reactive.function.client.WebClientRequestException || exception is IOException || exception is TimeoutException) {
      return true
    }

    return exception.cause?.let(::isRetryableException) ?: false
  }
}
