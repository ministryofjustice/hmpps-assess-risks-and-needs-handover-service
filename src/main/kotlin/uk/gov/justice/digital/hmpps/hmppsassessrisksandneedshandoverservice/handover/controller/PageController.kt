package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.handover.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config.AppConfiguration

@Controller
class PageController(
  val appConfiguration: AppConfiguration,
) {
  @GetMapping("/access-denied")
  fun accessDenied(): ModelAndView {
    return ModelAndView("error/access-denied").apply {
      addObject("oasysUrl", appConfiguration.services.oasys.baseUrl)
    }
  }
}
