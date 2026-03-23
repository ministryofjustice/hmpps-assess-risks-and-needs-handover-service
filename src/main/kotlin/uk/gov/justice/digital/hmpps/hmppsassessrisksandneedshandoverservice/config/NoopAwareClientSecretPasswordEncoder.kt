package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder

class NoopAwareClientSecretPasswordEncoder(
  private val delegate: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder(),
) : PasswordEncoder {
  override fun encode(rawPassword: CharSequence?): String = delegate.encode(rawPassword)
    ?: throw IllegalStateException("PasswordEncoder returned null")

  override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean = delegate.matches(rawPassword, encodedPassword)

  override fun upgradeEncoding(encodedPassword: String?): Boolean {
    if (encodedPassword?.startsWith("{noop}") == true) {
      return false
    }

    return delegate.upgradeEncoding(encodedPassword)
  }
}
