package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

class WireMockExtension : BeforeAllCallback, AfterAllCallback {
  companion object {
    // todo - is there a way to get this port from the confjg?
    val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(8089))
    lateinit var keyPair: KeyPair
    const val KEY_ID = "test-key-id"
  }

  override fun beforeAll(context: ExtensionContext) {
    wireMockServer.start()

    keyPair = generateRSAKeyPair()
    val jwksResponse = createJWKS(keyPair.public as RSAPublicKey, KEY_ID)

    wireMockServer.stubFor(
      get(urlPathEqualTo("/auth/.well-known/jwks.json"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(jwksResponse),
        ),
    )
  }

  override fun afterAll(context: ExtensionContext) {
    wireMockServer.stop()
  }

  private fun generateRSAKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    return keyPairGenerator.genKeyPair()
  }

  private fun createJWKS(publicKey: RSAPublicKey, keyId: String): String {
    val rsaKey = RSAKey.Builder(publicKey)
      .keyID(keyId)
      .build()
    val jwkSet = JWKSet(rsaKey)
    return jwkSet.toJSONObject().toString()
  }
}
