package uk.gov.justice.digital.hmpps.hmppsassesriskandneedshandoverservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("app.self")
class AppConfiguration() {
  var baseUrl: String = ""
  var endpoints: Endpoints = Endpoints()
}

class Endpoints {
  var handover: String? = null
}
