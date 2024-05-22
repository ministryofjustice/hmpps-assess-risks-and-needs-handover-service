package uk.gov.justice.digital.hmpps.hmppsassessriskandneedshandoverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableMethodSecurity
@SpringBootApplication
class HmppsAssessRiskAndNeedsHandoverService

fun main(args: Array<String>) {
  runApplication<HmppsAssessRiskAndNeedsHandoverService>(*args)
}
