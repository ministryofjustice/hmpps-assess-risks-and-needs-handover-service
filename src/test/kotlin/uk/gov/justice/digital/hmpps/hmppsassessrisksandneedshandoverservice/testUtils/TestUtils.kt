package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils

import net.datafaker.Faker
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SentencePlanContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.UserAccess
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.request.UpdateHandoverContextRequest
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.HandoverToken
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.entity.TokenStatus
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.CreateHandoverLinkRequest
import java.time.LocalDate
import java.util.UUID

object TestUtils {
  private var faker: Faker = Faker()

  private var objectMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()

  fun createHandoverRequest(): CreateHandoverLinkRequest = CreateHandoverLinkRequest(
    user = createPrincipal(),
    subjectDetails = createSubjectDetails(),
    oasysAssessmentPk = (100_000..999_999).random().toString(),
    assessmentVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
    sentencePlanVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
  )

  fun createHandoverRequestFromJson(): CreateHandoverLinkRequest = objectMapper.readValue(
    """
      {
        "user": {
          "identifier": "IDENTIFIER",
          "displayName": "Assessor TEST",
          "accessMode": "READ_ONLY",
          "planAccessMode": "READ_ONLY",
          "returnUrl": "http://test-oasys-return-url"
        },
        "subjectDetails": {
          "crn": "TP55229",
          "pnc": "01/126XXX",
          "nomisId": null,
          "givenName": "TestGivenName",
          "familyName": "TestSurname",
          "dateOfBirth": "1959-01-01",
          "gender": "1",
          "location": "COMMUNITY",
          "sexuallyMotivatedOffenceHistory": "NO"
        },
        "oasysAssessmentPk": 2164180,
        "sentencePlanVersion": "0",
        "criminogenicNeedsData": {
          "accommodation": {
            "accLinkedToHarm": "NO",
            "accLinkedToReoffending": "YES",
            "accStrengths": "NO",
            "accOtherWeightedScore": "6",
            "accThreshold": "YES"
          },
          "educationTrainingEmployability": {
            "eteLinkedToHarm": "NO",
            "eteLinkedToReoffending": "YES",
            "eteStrengths": "YES",
            "eteOtherWeightedScore": "2",
            "eteThreshold": "YES"
          },
          "finance": {
            "financeLinkedToHarm ": "NO",
            "financeLinkedToReoffending ": "NO",
            "financeStrengths": "NO",
            "financeOtherWeightedScore ": "N/A",
            "financeThreshold": "N/A"
          },
          "drugMisuse": {
            "drugLinkedToHarm": "NO",
            "drugLinkedToReoffending": "NO",
            "drugStrengths": "NO",
            "drugOtherWeightedScore": "0",
            "drugThreshold": "NO"
          },
          "alcoholMisuse": {
            "alcoholLinkedToHarm": "NO",
            "alcoholLinkedToReoffending": "YES",
            "alcoholStrengths": "YES",
            "alcoholOtherWeightedScore": "3",
            "alcoholThreshold": "YES"
          },
          "healthAndWellbeing": {
            "emoLinkedToHarm": "NO",
            "emoLinkedToReoffending": "NO",
            "emoStrengths": "NO",
            "emoOtherWeightedScore": "N/A",
            "emoThreshold": "N/A"
          },
          "personalRelationshipsAndCommunity": {
            "relLinkedToHarm": "NO",
            "relLinkedToReoffending": "NO",
            "relStrengths": "NO",
            "relOtherWeightedScore": "6",
            "relThreshold": "YES"
          },
          "thinkingBehaviourAndAttitudes ": {
            "thinkLinkedToHarm": "NO",
            "thinkLinkedToReoffending": "NO",
            "thinkStrengths": "NO",
            "thinkOtherWeightedScore": "10",
            "thinkThreshold": "YES"
          },
          "lifestyleAndAssociates ": {
            "lifestyleLinkedToHarm": "N/A",
            "lifestyleLinkedToReoffending": "N/A",
            "lifestyleStrengths": "N/A",
            "lifestyleOtherWeightedScore": "6",
            "lifestyleThreshold": "YES"
          }
        }
      }
    """.trimIndent(),
    CreateHandoverLinkRequest::class.java,
  )

  fun createHandoverContext(handoverSessionId: UUID): HandoverContext = HandoverContext(
    handoverSessionId = handoverSessionId,
    principal = createPrincipal(),
    subject = createSubjectDetails(),
    assessmentContext = createAssessmentContext(),
    sentencePlanContext = createSentencePlanContext(),
  )

  fun updateHandoverContextRequest(): UpdateHandoverContextRequest = UpdateHandoverContextRequest(
    principal = createPrincipal(),
    subject = createSubjectDetails(),
    assessmentContext = createAssessmentContext(),
    sentencePlanContext = createSentencePlanContext(),
  )

  fun createAssessmentContext() = AssessmentContext(
    oasysAssessmentPk = (100_000..999_999).random().toString(),
    assessmentId = UUID.randomUUID(),
    assessmentVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
  )

  fun createSentencePlanContext() = SentencePlanContext(
    oasysAssessmentPk = (100_000..999_999).random().toString(),
    planId = UUID.randomUUID(),
    planVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
  )

  fun createPrincipal() = HandoverPrincipal(
    identifier = faker.idNumber().valid(),
    displayName = faker.name().firstName(),
    accessMode = UserAccess.READ_WRITE,
    returnUrl = "http://test-oasys-return-url",
  )

  fun createSubjectDetails() = SubjectDetails(
    crn = "X${(100_000..999_999).random()}",
    pnc = "01/${(10_000_000..99_999_999).random()}A",
    nomisId = faker.idNumber().valid(),
    givenName = faker.name().firstName(),
    familyName = faker.name().lastName(),
    dateOfBirth = LocalDate.of(
      faker.number().numberBetween(1950, 2000),
      faker.number().numberBetween(1, 12),
      faker.number().numberBetween(1, 28),
    ),
    gender = listOf(0, 1, 2, 9).random(),
    location = listOf(Location.COMMUNITY, Location.PRISON).random(),
    sexuallyMotivatedOffenceHistory = listOf("YES", "NO").random(),
  )

  fun createEndPoint(): AppConfiguration.Self.Endpoints {
    val endpoints = AppConfiguration.Self.Endpoints()
    endpoints.handover = "/handover_endpoint"
    endpoints.context = "/context_endpoint"
    return endpoints
  }

  fun createHandoverToken(status: TokenStatus): HandoverToken = HandoverToken(
    tokenStatus = status,
    handoverSessionId = UUID.randomUUID(),
    principal = createPrincipal(),
  )
}
