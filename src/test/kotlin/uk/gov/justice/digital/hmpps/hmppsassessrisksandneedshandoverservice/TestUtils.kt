package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice

import net.datafaker.Faker
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.AssessmentContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverContext
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.HandoverPrincipal
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.Location
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.SubjectDetails
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity.UserAccess
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.request.HandoverRequest
import java.time.LocalDate
import java.util.UUID

object TestUtils {
  private var faker: Faker = Faker()

  fun createHandoverRequest(): HandoverRequest {
    return HandoverRequest(
      principal = createPrincipal(),
      subject = createSubjectDetails(),
      assessmentContext = createAssessmentContext(),
      sentencePlanContext = null,
    )
  }

  fun createHandoverContext(handoverSessionId: String): HandoverContext {
    return HandoverContext(
      id = UUID.randomUUID().toString(),
      handoverSessionId = handoverSessionId,
      principal = createPrincipal(),
      subject = createSubjectDetails(),
      assessmentContext = createAssessmentContext(),
      sentencePlanContext = null,
    )
  }

  private fun createAssessmentContext() = AssessmentContext(
    oasysAssessmentPk = faker.idNumber().valid(),
    assessmentUUID = UUID.randomUUID().toString(),
    assessmentVersion = "1",
  )

  private fun createPrincipal() = HandoverPrincipal(
    identifier = faker.idNumber().valid(),
    displayName = faker.name().fullName(),
    accessMode = UserAccess.READ_WRITE,
  )

  private fun createSubjectDetails() = SubjectDetails(
    crn = faker.idNumber().valid(),
    pnc = faker.idNumber().valid(),
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
    sexuallyMotivatedOffenceHistory = listOf("yes", "no").random(),
  )
}
