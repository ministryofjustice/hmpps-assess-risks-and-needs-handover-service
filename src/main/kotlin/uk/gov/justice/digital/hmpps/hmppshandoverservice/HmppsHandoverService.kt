package uk.gov.justice.digital.hmpps.hmppshandoverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableMethodSecurity
@SpringBootApplication
class HmppsHandoverService

fun main(args: Array<String>) {
  runApplication<HmppsHandoverService>(*args)
}
