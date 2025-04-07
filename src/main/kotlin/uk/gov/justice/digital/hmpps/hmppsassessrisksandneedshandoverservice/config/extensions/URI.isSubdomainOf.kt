package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.extensions

import java.net.URI

fun URI.isSubdomainOf(base: URI): Boolean = this.scheme == base.scheme &&
  this.port == base.port &&
  this.host.endsWith(".${base.host}")
