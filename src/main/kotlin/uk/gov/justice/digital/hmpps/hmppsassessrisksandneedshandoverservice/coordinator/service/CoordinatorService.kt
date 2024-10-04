package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.coordinator.response.AssociationsResponse

@Service
class CoordinatorService(
  val appConfiguration: AppConfiguration,
  val coordinatorApiWebClient: WebClient,
) {
  private fun endpoint(endpoint: String): String = "${appConfiguration.services.coordinatorApi.baseUrl}$endpoint"

  fun getAssociations(oasysAssessmentPk: String): AssociationsResponse {
    return try {
      val result = coordinatorApiWebClient.get()
        .uri(endpoint("/oasys/$oasysAssessmentPk/associations"))
        .retrieve()
        .bodyToMono(AssociationsResponse::class.java)
        .block()

      result ?: throw IllegalStateException("Unexpected empty associations response for OASys Assessment PK $oasysAssessmentPk")
    } catch (ex: WebClientResponseException) {
      throw Exception("Unexpected associations response code ${ex.statusCode}")
    } catch (ex: Exception) {
      throw Exception("Unexpected associations exception: ${ex.message}")
    }
  }
}
