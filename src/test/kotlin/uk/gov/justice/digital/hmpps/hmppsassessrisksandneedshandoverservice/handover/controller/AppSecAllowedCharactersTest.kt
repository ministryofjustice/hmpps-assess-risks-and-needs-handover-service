package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Accommodation
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AlcoholMisuse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.CriminogenicNeedsData
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.DrugMisuse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.EducationTrainingEmployability
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Finance
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HealthAndWellbeing
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.LifestyleAndAssociates
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.PersonalRelationshipsAndCommunity
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.ThinkingBehaviourAndAttitudes
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.UserAccess
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handlers.exceptions.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils.WireMockExtension
import java.time.LocalDate

@ExtendWith(WireMockExtension::class)
class AppSecAllowedCharactersTest : IntegrationTestBase() {
  @Autowired
  lateinit var appConfiguration: AppConfiguration

  @Test
  fun `should return bad request when request contains disallowed characters`() {
    val invalidString = "abc:D"

    val invalidRequest = CreateHandoverLinkRequest(
      user = HandoverPrincipal(
        identifier = invalidString,
        displayName = invalidString,
        accessMode = UserAccess.READ_WRITE,
        returnUrl = "http://test-oasys-return-url",
      ),
      subjectDetails = SubjectDetails(
        crn = invalidString,
        pnc = invalidString,
        nomisId = invalidString,
        givenName = invalidString,
        familyName = invalidString,
        dateOfBirth = LocalDate.of(1990, 1, 1),
        gender = 1,
        location = Location.PRISON,
        sexuallyMotivatedOffenceHistory = invalidString,
      ),
      oasysAssessmentPk = invalidString,
      assessmentVersion = 1,
      sentencePlanVersion = 1,
      criminogenicNeedsData = CriminogenicNeedsData(
        accommodation = Accommodation(
          accLinkedToHarm = invalidString,
          accLinkedToReoffending = invalidString,
          accStrengths = invalidString,
          accOtherWeightedScore = invalidString,
          accThreshold = invalidString,
        ),
        educationTrainingEmployability = EducationTrainingEmployability(
          eteLinkedToHarm = invalidString,
          eteLinkedToReoffending = invalidString,
          eteStrengths = invalidString,
          eteOtherWeightedScore = invalidString,
          eteThreshold = invalidString,
        ),
        finance = Finance(
          financeLinkedToHarm = invalidString,
          financeLinkedToReoffending = invalidString,
          financeStrengths = invalidString,
          financeOtherWeightedScore = invalidString,
          financeThreshold = invalidString,
        ),
        drugMisuse = DrugMisuse(
          drugLinkedToHarm = invalidString,
          drugLinkedToReoffending = invalidString,
          drugStrengths = invalidString,
          drugOtherWeightedScore = invalidString,
          drugThreshold = invalidString,
        ),
        alcoholMisuse = AlcoholMisuse(
          alcoholLinkedToHarm = invalidString,
          alcoholLinkedToReoffending = invalidString,
          alcoholStrengths = invalidString,
          alcoholOtherWeightedScore = invalidString,
          alcoholThreshold = invalidString,
        ),
        healthAndWellbeing = HealthAndWellbeing(
          emoLinkedToHarm = invalidString,
          emoLinkedToReoffending = invalidString,
          emoStrengths = invalidString,
          emoOtherWeightedScore = invalidString,
          emoThreshold = invalidString,
        ),
        personalRelationshipsAndCommunity = PersonalRelationshipsAndCommunity(
          relLinkedToHarm = invalidString,
          relLinkedToReoffending = invalidString,
          relStrengths = invalidString,
          relOtherWeightedScore = invalidString,
          relThreshold = invalidString,
        ),
        thinkingBehaviourAndAttitudes = ThinkingBehaviourAndAttitudes(
          thinkLinkedToHarm = invalidString,
          thinkLinkedToReoffending = invalidString,
          thinkStrengths = invalidString,
          thinkOtherWeightedScore = invalidString,
          thinkThreshold = invalidString,
        ),
        lifestyleAndAssociates = LifestyleAndAssociates(
          lifestyleLinkedToHarm = invalidString,
          lifestyleLinkedToReoffending = invalidString,
          lifestyleStrengths = invalidString,
          lifestyleOtherWeightedScore = invalidString,
          lifestyleThreshold = invalidString,
        ),
      ),
    )

    val response = webTestClient.post().uri(appConfiguration.self.endpoints.handover)
      .bodyValue(invalidRequest)
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer ${jwtHelper.generateAuthToken()}")
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody

    setOf(
      "user.identifier",
      "user.displayName",
      "subjectDetails.crn",
      "subjectDetails.pnc",
      "subjectDetails.nomisId",
      "subjectDetails.givenName",
      "subjectDetails.familyName",
      "subjectDetails.sexuallyMotivatedOffenceHistory",
      "oasysAssessmentPk",
      "criminogenicNeedsData.accommodation.accLinkedToHarm",
      "criminogenicNeedsData.accommodation.accLinkedToReoffending",
      "criminogenicNeedsData.accommodation.accStrengths",
      "criminogenicNeedsData.accommodation.accOtherWeightedScore",
      "criminogenicNeedsData.accommodation.accThreshold",
      "criminogenicNeedsData.educationTrainingEmployability.eteLinkedToHarm",
      "criminogenicNeedsData.educationTrainingEmployability.eteLinkedToReoffending",
      "criminogenicNeedsData.educationTrainingEmployability.eteStrengths",
      "criminogenicNeedsData.educationTrainingEmployability.eteOtherWeightedScore",
      "criminogenicNeedsData.educationTrainingEmployability.eteThreshold",
      "criminogenicNeedsData.finance.financeLinkedToHarm",
      "criminogenicNeedsData.finance.financeLinkedToReoffending",
      "criminogenicNeedsData.finance.financeStrengths",
      "criminogenicNeedsData.finance.financeOtherWeightedScore",
      "criminogenicNeedsData.finance.financeThreshold",
      "criminogenicNeedsData.drugMisuse.drugLinkedToHarm",
      "criminogenicNeedsData.drugMisuse.drugLinkedToReoffending",
      "criminogenicNeedsData.drugMisuse.drugStrengths",
      "criminogenicNeedsData.drugMisuse.drugOtherWeightedScore",
      "criminogenicNeedsData.drugMisuse.drugThreshold",
      "criminogenicNeedsData.alcoholMisuse.alcoholLinkedToHarm",
      "criminogenicNeedsData.alcoholMisuse.alcoholLinkedToReoffending",
      "criminogenicNeedsData.alcoholMisuse.alcoholStrengths",
      "criminogenicNeedsData.alcoholMisuse.alcoholOtherWeightedScore",
      "criminogenicNeedsData.alcoholMisuse.alcoholThreshold",
      "criminogenicNeedsData.healthAndWellbeing.emoLinkedToHarm",
      "criminogenicNeedsData.healthAndWellbeing.emoLinkedToReoffending",
      "criminogenicNeedsData.healthAndWellbeing.emoStrengths",
      "criminogenicNeedsData.healthAndWellbeing.emoOtherWeightedScore",
      "criminogenicNeedsData.healthAndWellbeing.emoThreshold",
      "criminogenicNeedsData.personalRelationshipsAndCommunity.relLinkedToHarm",
      "criminogenicNeedsData.personalRelationshipsAndCommunity.relLinkedToReoffending",
      "criminogenicNeedsData.personalRelationshipsAndCommunity.relStrengths",
      "criminogenicNeedsData.personalRelationshipsAndCommunity.relOtherWeightedScore",
      "criminogenicNeedsData.personalRelationshipsAndCommunity.relThreshold",
      "criminogenicNeedsData.thinkingBehaviourAndAttitudes.thinkLinkedToHarm",
      "criminogenicNeedsData.thinkingBehaviourAndAttitudes.thinkLinkedToReoffending",
      "criminogenicNeedsData.thinkingBehaviourAndAttitudes.thinkStrengths",
      "criminogenicNeedsData.thinkingBehaviourAndAttitudes.thinkOtherWeightedScore",
      "criminogenicNeedsData.thinkingBehaviourAndAttitudes.thinkThreshold",
      "criminogenicNeedsData.lifestyleAndAssociates.lifestyleLinkedToHarm",
      "criminogenicNeedsData.lifestyleAndAssociates.lifestyleLinkedToReoffending",
      "criminogenicNeedsData.lifestyleAndAssociates.lifestyleStrengths",
      "criminogenicNeedsData.lifestyleAndAssociates.lifestyleOtherWeightedScore",
      "criminogenicNeedsData.lifestyleAndAssociates.lifestyleThreshold",
    ).forEach {
      assertThat(response?.userMessage).contains("$it: Field must contain only alphanumeric characters, hyphens, spaces, commas, full stops, forward slashes, or apostrophes")
    }
  }
}
