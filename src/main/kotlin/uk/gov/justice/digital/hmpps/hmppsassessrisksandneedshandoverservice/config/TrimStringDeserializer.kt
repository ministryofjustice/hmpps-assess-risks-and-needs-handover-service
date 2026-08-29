package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

class TrimStringDeserializer : JsonDeserializer<String>() {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): String = p.valueAsString.trim()
}
