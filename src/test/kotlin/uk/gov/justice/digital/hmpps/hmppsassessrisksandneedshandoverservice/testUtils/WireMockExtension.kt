package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.testUtils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class WireMockExtension :
  BeforeAllCallback,
  AfterAllCallback {
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

    wireMockServer.stubFor(
      post(WireMock.urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "token_type": "bearer",
                "access_token": "ABCDE",
                "expires_in": ${LocalDateTime.now().plusHours(2).toEpochSecond(ZoneOffset.UTC)}
              }
              """.trimIndent(),
            ),
        ),
    )

    wireMockServer.stubFor(
      get(urlMatching("/oasys/.*/associations"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(createAssociationsResponse()),
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
    return jwkSet.toString()
  }

  private fun createAssociationsResponse(): String = """
      {
        "sentencePlanId": "${UUID.randomUUID()}",
        "sanAssessmentId": "${UUID.randomUUID()}"
      }
  """.trimIndent()
}
