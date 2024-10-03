package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils

import net.datafaker.Faker
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

  fun createHandoverRequest(): CreateHandoverLinkRequest {
    return CreateHandoverLinkRequest(
      user = createPrincipal(),
      subjectDetails = createSubjectDetails(),
      oasysAssessmentPk = (100_000..999_999).random().toString(),
      assessmentVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
      planVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
    )
  }

  fun createHandoverContext(handoverSessionId: UUID): HandoverContext {
    return HandoverContext(
      handoverSessionId = handoverSessionId,
      principal = createPrincipal(),
      subject = createSubjectDetails(),
      assessmentContext = createAssessmentContext(),
      sentencePlanContext = createSentencePlanContext(),
    )
  }

  fun updateHandoverContextRequest(): UpdateHandoverContextRequest {
    return UpdateHandoverContextRequest(
      principal = createPrincipal(),
      subject = createSubjectDetails(),
      assessmentContext = createAssessmentContext(),
      sentencePlanContext = createSentencePlanContext(),
    )
  }

  fun createAssessmentContext() = AssessmentContext(
    sanAssessmentId = UUID.randomUUID(),
    oasysAssessmentPk = (100_000..999_999).random().toString(),
    assessmentId = UUID.randomUUID(),
    assessmentVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
  )

  fun createSentencePlanContext() = SentencePlanContext(
    sentencePlanId = UUID.randomUUID(),
    oasysAssessmentPk = (100_000..999_999).random().toString(),
    planId = UUID.randomUUID(),
    planVersion = faker.number().numberBetween(Long.MIN_VALUE, Long.MAX_VALUE),
  )

  fun createPrincipal() = HandoverPrincipal(
    identifier = faker.idNumber().valid(),
    displayName = faker.name().fullName(),
    accessMode = UserAccess.READ_WRITE,
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

  fun createHandoverToken(status: TokenStatus): HandoverToken {
    return HandoverToken(
      tokenStatus = status,
      handoverSessionId = UUID.randomUUID(),
      principal = createPrincipal(),
    )
  }
}
