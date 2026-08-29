package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.context.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class HandoverContextTest {
  private var mapper = ObjectMapper()
    .registerKotlinModule()

  @Nested
  inner class HandoverPrincipalTest {
    @Test
    fun `trims whitespace from displayName`() {
      val serialized: String = """{
          "displayName": " \n\r Arlen Dovek\r  "
      }
      """.trimIndent()

      val deserialized = mapper.readValue(serialized, HandoverPrincipal::class.java)

      assertEquals("Arlen Dovek", deserialized.displayName)
    }

    @Test
    fun `ignores whitespace in the middle of displayName`() {
      val serialized: String = """{
        "displayName": " \n\r Arlen \n Dovek\r  "
      }
      """.trimIndent()

      val deserialized = mapper.readValue(serialized, HandoverPrincipal::class.java)

      assertEquals("Arlen \n Dovek", deserialized.displayName)
    }
  }

  @Nested
  inner class SubjectDetailsTest {
    @Test
    fun `trims whitespace from givenName and familyName`() {
      val serialized: String = """{
          "givenName": " \n\r Arlen \r  ",
          "familyName": " \r\n Dovek \n ",
          "gender": 0,
          "location": "COMMUNITY"
      }
      """.trimIndent()

      val deserialized = mapper.readValue(serialized, SubjectDetails::class.java)

      assertEquals("Arlen", deserialized.givenName)
      assertEquals("Dovek", deserialized.familyName)
    }

    @Test
    fun `ignores whitespace in the middle of givenName and familyName`() {
      val serialized: String = """{
          "givenName": "Arlen Bram",
          "familyName": "Cinder Dovek",
          "gender": 0,
          "location": "COMMUNITY"
      }
      """.trimIndent()

      val deserialized = mapper.readValue(serialized, SubjectDetails::class.java)

      assertEquals("Arlen Bram", deserialized.givenName)
      assertEquals("Cinder Dovek", deserialized.familyName)
    }
  }
}
