package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.validators

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration

class AllowedUrlValidatorTest {

  private lateinit var validator: AllowedUrlValidator
  private val appConfiguration: AppConfiguration = mockk(relaxed = true)
  private val oasysServiceConfig: AppConfiguration.Services.OasysService = mockk(relaxed = true)
  private val constraintValidatorContext: ConstraintValidatorContext = mockk(relaxed = true)

  @BeforeEach
  fun setUp() {
    every { oasysServiceConfig.returnUrls } returns listOf("https://approved-url.local", "https://another-approved-url.local")
    every { appConfiguration.services.oasys } returns oasysServiceConfig

    validator = AllowedUrlValidator(appConfiguration)
  }

  @Test
  fun `should return true for an approved URL`() {
    val validUrl = "https://approved-url.local/ords/1234"

    val result = validator.isValid(validUrl, constraintValidatorContext)

    assertTrue(result, "Expected URL to be valid as it matches an approved prefix")
    verify { oasysServiceConfig.returnUrls }
  }

  @Test
  fun `should return false for an unapproved URL`() {
    val invalidUrl = "https://unapproved-url.local/not-approved"

    val result = validator.isValid(invalidUrl, constraintValidatorContext)

    assertFalse(result, "Expected URL to be invalid as it does not match any approved prefix")
    verify { oasysServiceConfig.returnUrls }
  }

  @Test
  fun `should return false when URL is null`() {
    val result = validator.isValid(null, constraintValidatorContext)

    assertFalse(result, "Expected null URL to be invalid")
    verify(exactly = 0) { oasysServiceConfig.returnUrls }
  }
}
