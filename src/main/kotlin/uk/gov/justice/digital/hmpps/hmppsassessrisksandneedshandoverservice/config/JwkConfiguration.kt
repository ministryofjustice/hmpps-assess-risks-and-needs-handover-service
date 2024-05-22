package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMDecryptorProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.StringReader
import java.security.KeyPair
import java.security.Security
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver.jwk")
class JwkProperties(
  private var pemEncoded: String,
  private var pemSecret: String,
) {
  fun decode(): KeyPair {
    try {
      // Decode from base64
      val decodedPem = String(Base64.getDecoder().decode(pemEncoded))
      // Parse Pem
      val parsePem = PEMParser(StringReader(decodedPem)).readObject() as PEMEncryptedKeyPair
      // Decrypt Pem
      val decryptor: PEMDecryptorProvider = JcePEMDecryptorProviderBuilder().build(pemSecret.toCharArray())
      val decryptedPem = parsePem.decryptKeyPair(decryptor)
      return JcaPEMKeyConverter().getKeyPair(decryptedPem)
    } catch (e: Exception) {
      throw IllegalStateException("JWK PEM is corrupt or secret is incorrect")
    }
  }
}

@EnableConfigurationProperties(JwkProperties::class)
@Configuration
class JwkConfiguration {

  init {
    Security.addProvider(BouncyCastleProvider())
  }

  @Bean
  fun jwkSource(
    jwkProperties: JwkProperties,
  ): JWKSource<SecurityContext> {
    val keyPair = jwkProperties.decode()
    val publicKey = keyPair.public as RSAPublicKey
    val privateKey = keyPair.private as RSAPrivateKey
    val rsaKey: RSAKey = RSAKey.Builder(publicKey)
      .privateKey(privateKey)
      .keyID("key-id")
      .build()
    return ImmutableJWKSet(JWKSet(rsaKey))
  }
}
