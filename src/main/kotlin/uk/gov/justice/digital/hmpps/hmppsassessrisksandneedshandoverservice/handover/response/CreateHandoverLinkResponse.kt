package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response

import java.io.Serializable

class CreateHandoverLinkResponse(
  val handoverLink: String,
  val handoverSessionId: String,
) : Serializable
