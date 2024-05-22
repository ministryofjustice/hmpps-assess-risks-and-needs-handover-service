package uk.gov.justice.digital.hmpps.hmppsassesriskandneedshandoverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableMethodSecurity
@SpringBootApplication
class HmppsAssesRiskAndNeedsHandoverService

fun main(args: Array<String>) {
  runApplication<HmppsAssesRiskAndNeedsHandoverService>(*args)
}
