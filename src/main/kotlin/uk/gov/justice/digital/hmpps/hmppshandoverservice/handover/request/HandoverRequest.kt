package uk.gov.justice.digital.hmpps.hmppshandoverservice.handover.request

import uk.gov.justice.digital.hmpps.hmppshandoverservice.context.entity.HandoverPrincipal

class HandoverRequest(
  val principal: HandoverPrincipal,
  val subject: Any?,
  val assessmentContext: Any?,
  val sentencePlanContext: Any?,
)
