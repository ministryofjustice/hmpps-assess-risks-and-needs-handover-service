package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.authorization.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
abstract class HandoverPrincipalMixin
