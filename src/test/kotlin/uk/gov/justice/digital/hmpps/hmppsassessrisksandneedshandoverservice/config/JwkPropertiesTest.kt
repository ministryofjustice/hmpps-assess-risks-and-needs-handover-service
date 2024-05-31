package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.hibernate.validator.internal.util.Contracts.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.KeyPair
import java.security.Security

class JwkPropertiesTest {

  init {
    Security.addProvider(BouncyCastleProvider())
  }

  private val testPemSecret = "THIS_IS_A_TEST_PEM"
  private val testPemEncoded = "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpQcm9jLVR5cGU6IDQsRU5DUllQVEV" +
    "ECkRFSy1JbmZvOiBBRVMtMjU2LUNGQixEMTlBQzk2QUQ4NkYyODIwMzE1MjY3NDYyMDA4NzExNwoKOHE2TWtQSzdoM2M4enRja" +
    "2UrSWpEQVFvZXdidlRZaW5yd05vaktoVlI4MnMveFRLT0dpUm5Ea2V5MGdmTGtCZwpISUsrQVM3YWp6TmVkOHhGbzNVYVpwRVF" +
    "TRnM5alQ4bmNHSDQrTVpSM291dE5lbzdGNW9Yc1RSVlIzRkpUNTdqClJ6RXFKYkUzd0ppZVI3dmdqdU81bmpHWGdhZzQ0UGVhV" +
    "0sya2ZJK2MvRU9HVm1GSGNSWm51UWFDSG9xZG9KN1EKYzNLVjlJSUM1MWtETUJldU16SE0wSzY0MTVyUEkwNm5KNWwzOEdyNDE" +
    "rSDlIMmZHL3UwRWtqM2cvSFUyNVpsYQpaZmMxclFRUnVkNFc2UDltSnA1UGY1MUVhOGJEbHZIRyttK1M3Q00rZFN1VlgvZzJaY" +
    "WpTcW9jaTZ3UVZuVTlIClNrL2tQTTI2QVpSdDNiSjgvZ3RzbklKZ3VaY2o1aVk5ZWFEQkNSandidXpRUHVTbkJsNVVvdkRkZzd" +
    "POVoybTQKcVdaTjNEL0NKRkxTcHFkY2MyQWNyZnlQT2YyVVc5eFBVT25CR1QyTDhTVXJSMmdzakVCTTVtalM2cFBUcUtXRApqZ" +
    "ng0b2tkVndad3h4QTJvK3Z1bzFpcW5qZFIxSVlDUDBxeXg3eEE2Z3BtU0RnaEl4SmF4VlErSG1YRGJKSlc5CnMvS2JXUXgyM0l" +
    "LMUwya2lrVTF1dW40dzdRakIybmM2eWpjeTNQbUJyemYxem5MZHJxY2hOWXNJdzFPSE9HSlgKSVRZK3VCZ2xIQTdGd0pWclVpW" +
    "GM5cit4RkR4QlZFQXRlMXN5clZLTy9vVllwY2xoVk1XZjB0Q1l0aEdNTkdkUwpSaXNDVE9ReG1zbGRxQVRFZFlxVVhrM2R4bm5" +
    "VSWdDQkdpQmFFSkppM2svQWdTQ1h5cXpWRGxhbXh5OW9qZkZ1CkxuUWk5SnJpU2FYZGtNVW9haWxENWxTUWNoeGFQUVNWU3JCa" +
    "ExXTTFlMWdTdjNDV2wwTlFxS1V5cUpUa09GM2oKenBLa2xzdkpMcGtCN2RKeVkzUUdhaFh6QTdsOEwxNHo2bjZ4emwxRjRVR2J" +
    "ESnVEVFM4L0E1VTJGMjlFakdEZAorZzdacUVRdTJBMGYxOEowYnZQbUo3UEY4MnlNMGFlU2dNNU1HSWF5cmJiZXg3SUZPZnFaW" +
    "GVLa3hpRkhqUVlhCnduNFMyMlZXRkRZR1R1WXAyejhQU3RNc2RaRGNoUDhLTzh1ZzlCdjhoRWg4dTNtaGZQdE5LclhGZ1lJOUZ" +
    "IMVUKTWNGYmRsa3B4Nlh2a3BkczEyOXM1Z0FReWVwM3RDWDh2VEplY1VVZG9aaGVSYVJnNXhTZTFxd1VpYTRSd1ZuVQpVeW1wd" +
    "W9iTXZ0N2NEZlBBeWszWFJLT0dtTWp0RlNyQzNHNVhGQTJTVDZIQUV1Umo1cjY4bGFWSFMwdlZzWmpDCjlkQ3Z5SWVHQ1ZFd0I" +
    "2Q3RicnFHSkU4RTR3QWNqdUsvdkhoRG5VcmtBakFLTFNFdWdpMEgzNXBlYnJCNTRaNDgKQlRIejJKUVd0SURWWCtvRnI0T1ljQ" +
    "lFNekJNSTZpckl3L013cDRtbWM5MlMxdndYK1pDWExMMGd1ZE5SdUFvaApaN0pVbVNUMHhMeG4vQXN1Kzd4OG51WEk2bStjbjd" +
    "VNjV3Mms2cmJzSm5NbFZIeFhVZzVaekQ0anZDaFl6TDJKCjJWQVRHR285N0FQUlRVQXdUUkMyYVp5c2c2VFpmL0ZIQXJGMld5a" +
    "nZ0RGR4dmVSMjBpVzZyWlIzZTVkMzM1MWcKUEtwUUVNT3BFcENYL3Z4akluUUtFRXNma1RRc0VKZkFDcXYzdXRjdFVHcnpscGd" +
    "PdklHUXA1dUdZZHQrUXhwZApQTlJPSTBUcWN1aldsbU5ianhyUXpvUUFPbitPS2E2b0d4d2tJcHg0SHNwejNORGN0U2dDVzBBV" +
    "0V4d1BkcHIrClhNWEt1ck1qcUZOeS9xWVRHaXdYMkpCcGdBQzl0R0o0NmhWNUhSZjJ1bHRYSzRlSi93alRtaWJhWkJNVEx4ZFc" +
    "KRUJzcmJnK3d3YlJDcFNWL1I0eVNLQ3ZzNUxwem44elZXc3VNU2x2a2twdjdzYVF3Uk14VAotLS0tLUVORCBSU0EgUFJJVkFUR" +
    "SBLRVktLS0tLQotLS0tLUJFR0lOIFBVQkxJQyBLRVktLS0tLQpNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJ" +
    "DZ0tDQVFFQXl5TEo2SXVFSGJRT2JHVU9PaHZQCjhqR0dHVkdDVWFMdU9lb0FWZUFUVlJTRitta2dUSHFFNDc0Mk1KNXNQSVlUK" +
    "3l2Slg3a0xSYkZ3d1JjTVFleWEKRWJqSU5Cb2RLRkNvTm9RWHExNVRlU3VCOFY4Vlk2N2R3Q0Nya1pudGlhZXJCTTFid21BZ1J" +
    "zWEorMzlVSG1ISgpvQjVuUXpybU95YmZLNC9BcGZncW1EM2lhWWx1dDBETVYwOVl6bnc4Q01Fc1N5WHZkVEIzTkF0Tm5TM0Zze" +
    "jBFCjJHZy9QSDNTSTZURFlqcSt6NFB2SCtlQTUxd0xSV3JsRnhzWDlQUTRCT255TWRRQnY3b1RxQm1hVjZKeFlrb3oKUEI1eFl" +
    "LcFpsWnlYRFpOcE00T0wrT3cvTXJJbHdQeWxmaDY2a3NVQ2VVcGNtQmpnSkIvOURUeXNDNHdQejhVeQpWUUlEQVFBQgotLS0tL" +
    "UVORCBQVUJMSUMgS0VZLS0tLS0K"

  @Test
  fun `decode should return KeyPair when given valid PEM`() {
    val jwkProperties = JwkProperties(testPemEncoded, testPemSecret)

    val keyPair: KeyPair = jwkProperties.decode()

    assertNotNull(keyPair)
    assertNotNull(keyPair.public)
    assertNotNull(keyPair.private)
  }

  @Test
  fun `decode should throw IllegalStateException when given wrong secret for PEM`() {
    val pemSecret = "wrong_secret"
    val jwkProperties = JwkProperties(testPemEncoded, pemSecret)

    assertThrows<IllegalStateException> {
      jwkProperties.decode()
    }
  }

  @Test
  fun `decode should throw IllegalStateException when given invalid PEM`() {
    val invalidPemEncoded = "invalid_base64_encoded_pem"
    val pemSecret = "wrong_secret"
    val jwkProperties = JwkProperties(invalidPemEncoded, pemSecret)

    // Act & Assert
    assertThrows<IllegalStateException> {
      jwkProperties.decode()
    }
  }
}
