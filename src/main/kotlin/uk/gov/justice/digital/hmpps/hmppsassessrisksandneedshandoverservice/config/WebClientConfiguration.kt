package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${app.services.coordinator-api.base-url}") val coordinatorApiUri: String,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun coordinatorApiWebClient(authorizedClientManager: org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager, builder: WebClient.Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "coordinator-api", url = coordinatorApiUri, timeout)
}
