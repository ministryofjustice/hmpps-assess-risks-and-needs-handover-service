package uk.gov.justice.digital.hmpps.hmppshandoverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsHandoverService

fun main(args: Array<String>) {
  runApplication<HmppsHandoverService>(*args)
}
