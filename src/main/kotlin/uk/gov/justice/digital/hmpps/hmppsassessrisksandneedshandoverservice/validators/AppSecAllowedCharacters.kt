package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.validators

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import jakarta.validation.constraints.Pattern
import kotlin.reflect.KClass

@Pattern(regexp = "^[a-zA-Z0-9-'\\s,./]+$")
@Constraint(validatedBy = [])
@ReportAsSingleViolation
annotation class AppSecAllowedCharacters(
  val message: String = "Field must contain only alphanumeric characters, hyphens, spaces, commas, full stops, forward slashes, or apostrophes",
  val groups: Array<KClass<Any>> = [],
  val payload: Array<KClass<Payload>> = [],
)
