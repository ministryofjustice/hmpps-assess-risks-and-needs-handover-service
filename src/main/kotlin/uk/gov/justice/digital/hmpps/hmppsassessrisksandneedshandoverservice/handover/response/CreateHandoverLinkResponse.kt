package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.response

import java.io.Serializable
import java.util.UUID

class CreateHandoverLinkResponse(
  val handoverLink: String,
  val handoverSessionId: UUID,
  val link: String,
) : Serializable
