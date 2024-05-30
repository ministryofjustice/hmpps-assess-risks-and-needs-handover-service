package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.*
import java.time.LocalDate

@AutoConfigureWebTestClient(timeout = "360000000")
@DisplayName("Handover context Tests")
class HandoverContextControllerTest : IntegrationTestBase() {

  private val goalRequestBody = HandoverContext(
    handoverSessionId = "sessionId",
    principal =  HandoverPrincipal(),
    assessmentContext = AssessmentContext(
      oasysAssessmentPk = "abc",
      assessmentUUID = "ok",
      assessmentVersion= "1.0",
    ),
    sentencePlanContext = SentencePlanContext(
      oasysPk = "ok",
      assessmentVersion = "1.0"
    ),
    subject = SubjectDetails(
      givenName = "some name",
      crn = "crn",
      nomisId = "id",
      familyName = "xyz",
      dateOfBirth= LocalDate.now(),
      gender = 1,
      location = Location.PRISON,
      sexuallyMotivatedOffenceHistory = "history",
      pnc = "pnc",

    )
  )

  @Test
  fun `Update the handover context should return unauthorized when no auth token`() {
    webTestClient.post().uri("/context/abc")
      .header("Content-Type", "application/json")
      .bodyValue(goalRequestBody)
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
//      .bodyValue(goalRequestBody)
//      .exchange()
//      .expectStatus().isForbidden
//  }
}