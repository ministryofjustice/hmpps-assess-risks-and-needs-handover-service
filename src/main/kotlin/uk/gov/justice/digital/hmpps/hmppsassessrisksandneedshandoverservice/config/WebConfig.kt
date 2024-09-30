package uk.gov.justice.digital.hmpps.hmppsassessrisksandneedshandoverservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.WebJarsResourceResolver

@Configuration
class WebConfig: WebMvcConfigurer {
  override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    registry
      .addResourceHandler("/webjars/**")
      .addResourceLocations("/webjars/")
      .resourceChain(true)
      .addResolver(WebJarsResourceResolver())
  }
}
