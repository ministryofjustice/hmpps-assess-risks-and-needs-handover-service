package uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.response

import java.io.Serializable

class CreateHandoverLinkResponse(
  val handoverLink: String,
  val handoverSessionId: String,
) : Serializable
