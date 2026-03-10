package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.response.AssociationsResponse

@Service
class CoordinatorService(
  private val coordinatorApiWebClient: WebClient,
) {
  fun getAssociations(oasysAssessmentPk: String, planVersion: Long?): AssociationsResponse = try {
    val result = coordinatorApiWebClient.get()
      .uri { uriBuilder ->
        uriBuilder.path("/oasys/$oasysAssessmentPk/associations").apply {
          if (planVersion !== null) queryParam("planVersion", planVersion)
        }.build()
      }
      .retrieve()
      .bodyToMono(AssociationsResponse::class.java)
      .block()

    result
      ?: throw IllegalStateException("Unexpected empty associations response for OASys Assessment PK $oasysAssessmentPk")
  } catch (ex: WebClientResponseException) {
    throw Exception("Unexpected associations response code ${ex.statusCode}", ex)
  } catch (ex: Exception) {
    throw Exception("Unexpected associations exception: ${ex.message}", ex)
  }
}
