package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableMethodSecurity
@SpringBootApplication
class HmppsAssessRisksAndNeedsHandoverService

fun main(args: Array<String>) {
  runApplication<HmppsAssessRisksAndNeedsHandoverService>(*args)
}
