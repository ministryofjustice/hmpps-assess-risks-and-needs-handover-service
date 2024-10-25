package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.validators

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [AllowedUrlValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class OasysReturnUrl(
  val message: String = "OASys return URL is not on approved list",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

@Component
class AllowedUrlValidator(
  private val appConfiguration: AppConfiguration,
) : ConstraintValidator<OasysReturnUrl, String> {

  override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
    return value != null && appConfiguration.services.oasys.returnUrls.any { value.startsWith(it) }
  }
}
