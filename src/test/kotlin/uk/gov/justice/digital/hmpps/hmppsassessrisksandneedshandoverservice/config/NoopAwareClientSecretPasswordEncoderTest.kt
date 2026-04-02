package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class NoopAwareClientSecretPasswordEncoderTest {
  @Test
  fun `should not upgrade noop encoded secrets`() {
    val delegate = mockk<PasswordEncoder>()
    val encoder = NoopAwareClientSecretPasswordEncoder(delegate)

    val result = encoder.upgradeEncoding("{noop}secret")

    assertFalse(result)
  }

  @Test
  fun `should delegate upgrade check for non noop secrets`() {
    val delegate = mockk<PasswordEncoder>()
    every { delegate.upgradeEncoding("{bcrypt}secret") } returns true
    val encoder = NoopAwareClientSecretPasswordEncoder(delegate)

    val result = encoder.upgradeEncoding("{bcrypt}secret")

    assertTrue(result)
    verify(exactly = 1) { delegate.upgradeEncoding("{bcrypt}secret") }
  }

  @Test
  fun `should delegate secret matching`() {
    val delegate = mockk<PasswordEncoder>()
    every { delegate.matches("secret", "{noop}secret") } returns true
    val encoder = NoopAwareClientSecretPasswordEncoder(delegate)

    val result = encoder.matches("secret", "{noop}secret")

    assertTrue(result)
    verify(exactly = 1) { delegate.matches("secret", "{noop}secret") }
  }
}
